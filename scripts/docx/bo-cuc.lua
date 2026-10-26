-- Bộ lọc pandoc cho scripts/xuat-docx-api.js, chạy trước do-rong-bang.lua. Sắp 1 tài liệu docs/api/*.md theo bố cục
-- của eClinic_API_XacThuc_TaiKhoan.docx; bản Markdown giữ thứ tự của nó. Markdown: # tên tài liệu, ## phần (1 quy ước,
-- 2 route dùng chung, 3 / 4 phần của vai trò), ### mục, #### route hoặc mô hình. Bản .docx:
--   - bìa: "eClinic Backend", "Tài liệu API", vai trò (phần sau " — " của #), bảng thông tin;
--   - 1. Giới thiệu và phạm vi: các đoạn đầu tài liệu, đoạn bố cục, lịch sử phiên bản;
--   - 2. Danh sách route: dựng từ tiêu đề route và các bảng "Tất cả route của mục N" (các bảng này không in ra);
--   - 3. Quy ước chung: Markdown 1.1–1.7, rồi tổng quan của từng phần vai trò (x.1 và phần enum đầu x.2);
--   - 4. Mô hình dữ liệu: mô hình của 1.8 (đoạn **`Ten`**) và của x.2, mỗi mô hình 1 tiêu đề 4.n;
--   - 5 trở đi: mỗi mục 2.1–2.3, x.3… thành 1 mục cấp 1; mỗi route là tiêu đề "N.k <Mục đích>" và dải phương thức +
--     đường dẫn (style ThanhRoute), rồi khối route theo tài liệu cũ: "Quyền truy cập: <vai trò>" (câu đầu của
--     **Quyền:**, style QuyenTruyCap), đoạn mô tả, nhãn REQUEST… trước khối request đầu tiên, RESPONSE THÀNH CÔNG — <mã>,
--     MÃ LỖI + bảng HTTP | maLoi | Điều kiện đọc từ dòng **Lỗi:** (dòng không đọc được giữ nguyên chữ). Nhãn dùng style
--     NhanKhoi;
--   - Phụ lục A: bảng maLoi | HTTP | Thông điệp của mọi mã trong code (MaLoi.java + mã riêng của gateway, nhận từ
--     scripts/xuat-docx-api.js qua biến môi trường MA_LOI_JSON) mà tài liệu nhắc tới dưới dạng chữ code;
--   - Phụ lục B: các luồng của danh sách **… luồng chính** ở mục tổng quan từng phần vai trò, mỗi luồng 1 tiêu đề "B.k";
--   - Phụ lục C: phần từ nhãn **Chạy thử và tài khoản mẫu** tới hết mục 1.1 (mục 3.1 giữ địa chỉ gateway và bảng tiền tố).
-- Mọi id của Markdown được giữ (tiêu đề bị bỏ để lại Span rỗng mang id) nên liên kết vẫn tới đúng chỗ. "mục N" trong chữ
-- của Markdown được đổi sang số mới; số không có trong bảng đổi thì dừng xuất. Bảng do bộ lọc dựng mang class co-dinh và
-- có sẵn độ rộng cột, do-rong-bang.lua bỏ qua.
local MAU_PHUONG_THUC = { GET = '2E7D32', POST = '1565C0', PUT = 'E65100', DELETE = 'C62828', PATCH = '6A1B9A' }
local CAN_CU = 'Mã nguồn backend eClinic (controller, DTO, MaLoi, application.yml); route đối chiếu bằng '
  .. 'scripts/kiem-tra-tai-lieu-api.js'
local GACH = '–'
local str = pandoc.utils.stringify

local function loi(...)
  error('bo-cuc.lua: ' .. table.concat({ ... }), 0)
end

local function chu(s)
  local kq = pandoc.Inlines {}
  for tu in s:gmatch('%S+') do
    if #kq > 0 then kq:insert(pandoc.Space()) end
    kq:insert(pandoc.Str(tu))
  end
  return kq
end

local function la_cach(x)
  return x and (x.t == 'Space' or x.t == 'SoftBreak')
end

-- Nối các đoạn chữ (string hoặc Inlines), cách nhau 1 dấu cách
local function noi(...)
  local kq = pandoc.Inlines {}
  for _, phan in ipairs({ ... }) do
    local ils = type(phan) == 'string' and chu(phan) or pandoc.Inlines(phan)
    while la_cach(ils[1]) do ils:remove(1) end
    if #ils > 0 then
      if #kq > 0 then kq:insert(pandoc.Space()) end
      kq:extend(ils)
    end
  end
  return kq
end

local function moc(id)
  return pandoc.Span({}, pandoc.Attr(id))
end

local bo_code = { Code = function(c) return pandoc.Str(c.text) end }

local function noi_dung_o(o)
  local kq = pandoc.Inlines {}
  for _, b in ipairs(o.contents) do
    if b.t == 'Plain' or b.t == 'Para' then kq:extend(b.content) end
  end
  return kq
end

local function dong_than(bang)
  local kq = {}
  for _, than in ipairs(bang.bodies) do
    for _, d in ipairs(than.body) do kq[#kq + 1] = d end
  end
  return kq
end

local function dau_bang(b, ...)
  if b.t ~= 'Table' or #b.head.rows ~= 1 then return false end
  local o, can = b.head.rows[1].cells, { ... }
  for i, ten in ipairs(can) do
    if not o[i] or str(o[i].contents) ~= ten then return false end
  end
  return #o >= #can
end

local function la_nhan(b, dau)
  return b and b.t == 'Para' and #b.content == 1 and b.content[1].t == 'Strong'
    and str(b.content[1]):sub(1, #dau) == dau
end

-- Đoạn mở đầu 1 mô hình ở 1.8: **`Ten`** (có thể kèm chữ sau)
local function la_mo_hinh(b)
  local dam = b.t == 'Para' and b.content[1]
  return dam and dam.t == 'Strong' and #dam.content == 1 and dam.content[1].t == 'Code'
end

-- Đọc cây phần / mục / route. Bảng "Tất cả route của mục N" và nhãn của nó bị bỏ, nội dung bảng vào `route`.
local function doc_cay(blocks)
  local cay, route, hien = { dau = {}, phan = {} }, {}, nil
  hien = cay.dau
  for _, b in ipairs(blocks) do
    if b.t == 'Header' and b.level == 1 then
      cay.h1 = str(b.content)
    elseif b.t == 'Header' and b.level == 2 then
      if str(b.content) == 'Mục lục' then
        hien = {}
      else
        local p = { so = str(b.content):match('^(%d+)%. ') or loi('## không có số: ', str(b.content)), h = b, dau = {},
          muc = {} }
        table.insert(cay.phan, p)
        hien = p.dau
      end
    elseif b.t == 'Header' and b.level == 3 then
      local p = cay.phan[#cay.phan] or loi('### trước ## đầu tiên: ', str(b.content))
      local m = { so = str(b.content):match('^(%d+%.%d+) ') or loi('### không có số: ', str(b.content)), h = b,
        dau = {}, con = {} }
      if m.so:match('^%d+') ~= p.so then loi('### ', m.so, ' nằm trong ## ', p.so) end
      table.insert(p.muc, m)
      hien = m.dau
    elseif b.t == 'Header' and b.level == 4 then
      local p = cay.phan[#cay.phan]
      local m = p and p.muc[#p.muc] or loi('#### ngoài ###: ', str(b.content))
      local pt, dd = str(b.content):match('^(%u+) (/%S+)$')
      local c = { h = b, khoi = {} }
      if pt and MAU_PHUONG_THUC[pt] then c.pt, c.dd = pt, dd end
      table.insert(m.con, c)
      hien = c.khoi
    elseif b.t == 'Header' then
      loi('không dùng tiêu đề cấp ', b.level, ': ', str(b.content))
    elseif dau_bang(b, 'Phương thức', 'Đường dẫn', 'Quyền', 'Mục đích') then
      for _, d in ipairs(dong_than(b)) do
        route[str(d.cells[1].contents) .. ' ' .. str(d.cells[2].contents)] =
          { quyen = noi_dung_o(d.cells[3]), muc_dich = noi_dung_o(d.cells[4]) }
      end
      if la_nhan(hien[#hien], 'Tất cả route') then table.remove(hien) end
    else
      table.insert(hien, b)
    end
  end
  return cay, route
end

-- "mục N", "mục N–M", "mục N, M" (cả trong liên kết, chữ đậm) → số mới theo bảng `doi`
local function doc_so(s, i)
  local a = s:match('^%d+', i)
  if not a then return nil, i end
  i = i + #a
  local le = s:match('^%.%d+', i)
  if le then a, i = a .. le, i + #le end
  return a, i
end

local function doi_so(doi, s)
  local a, i = doc_so(s, 1)
  local b
  if s:sub(i, i + #GACH - 1) == GACH then
    b, i = doc_so(s, i + #GACH)
    if not b then loi('"mục ', s, '" không đọc được') end
  end
  local duoi = s:sub(i)
  if not duoi:match('^%p*$') then loi('"mục ', s, '" không đọc được') end
  for _, so in ipairs({ a, b }) do
    if not doi[so] then loi('"mục ', so, '" không có trong bảng đổi số mục') end
  end
  local dau, cuoi = doi[a][1], doi[b or a][2]
  return (dau == cuoi and dau or dau .. GACH .. cuoi) .. duoi, duoi
end

local function bo_doi_muc(doi)
  return {
    Inlines = function(ils)
      local i = 1
      while i <= #ils do
        local a, j = ils[i], i + 2
        if a.t == 'Str' and a.text:match('[Mm]ục$') and la_cach(ils[i + 1]) and ils[j] and ils[j].t == 'Str'
          and ils[j].text:match('^%d') then
          while true do
            local moi, duoi = doi_so(doi, ils[j].text)
            ils[j] = pandoc.Str(moi)
            local sau = ils[j + 2]
            if duoi ~= ',' or not la_cach(ils[j + 1]) or not sau or sau.t ~= 'Str'
              or not sau.text:match('^%d+%.%d+%p*$') then break end
            j = j + 2
          end
          i = j + 1
        else
          i = i + 1
        end
      end
      return ils
    end,
  }
end

local function huy_hieu(pt)
  return pandoc.RawInline('openxml', '<w:r><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:cs="Consolas"/>'
    .. '<w:b/><w:bCs/><w:color w:val="FFFFFF"/><w:sz w:val="16"/><w:szCs w:val="16"/>'
    .. '<w:shd w:val="clear" w:color="auto" w:fill="' .. MAU_PHUONG_THUC[pt] .. '"/></w:rPr>'
    .. '<w:t xml:space="preserve"> ' .. pt .. ' </w:t></w:r>')
end

local function kieu(ten, blocks)
  return pandoc.Div(blocks, pandoc.Attr('', {}, { ['custom-style'] = ten }))
end

local function o(x)
  return pandoc.Cell(pandoc.Blocks { pandoc.Plain(type(x) == 'string' and chu(x) or x) })
end

-- Bảng có sẵn độ rộng cột; dau = nil là bảng không có dòng tiêu đề
local function bang(cot, dau, dong)
  local colspecs, hang = {}, {}
  for i, c in ipairs(cot) do colspecs[i] = { c[2] and pandoc[c[2]] or pandoc.AlignDefault, c[1] } end
  for i, d in ipairs(dong) do hang[i] = pandoc.Row(d) end
  return pandoc.Table({ long = pandoc.Blocks {} }, colspecs, pandoc.TableHead(dau and { pandoc.Row(dau) } or {}),
    { { attr = pandoc.Attr(), body = hang, head = {}, row_head_columns = 0 } }, pandoc.TableFoot(),
    pandoc.Attr('', { 'co-dinh' }))
end

local function tieu_de(cap, so, ils, id)
  return pandoc.Header(cap, noi(so, ils), pandoc.Attr(id or ''))
end

-- Khối route --------------------------------------------------------------------------------------------------------

local NHAN_REQUEST = { query = 'REQUEST — QUERY', path = 'REQUEST — PATH', body = 'REQUEST BODY (JSON)',
  form = 'REQUEST (MULTIPART/FORM-DATA)', multipart = 'REQUEST (MULTIPART/FORM-DATA)' }
local TRANG_THAI = { ['200'] = '200 OK', ['201'] = '201 Created' }

local function nhan_khoi(s)
  return kieu('NhanKhoi', { pandoc.Para(chu(s)) })
end

-- Đoạn mở đầu bằng **Ten** (vd **Lỗi:**); trả phần chữ sau nhãn
local function sau_nhan(b, ten)
  if not (b and b.t == 'Para' and b.content[1] and b.content[1].t == 'Strong' and str(b.content[1]) == ten) then
    return nil
  end
  local ils = pandoc.Inlines { table.unpack(b.content, 2) }
  while la_cach(ils[1]) do ils:remove(1) end
  return ils
end

local function bo_cach_cuoi(ils)
  while la_cach(ils[#ils]) do ils:remove(#ils) end
  return ils
end

-- Nối lại các Str liền nhau (bộ phân tích tách Str theo dấu câu)
local function gop(ils)
  local kq = pandoc.Inlines {}
  for _, x in ipairs(ils) do
    local cuoi = kq[#kq]
    if x.t == 'Str' and cuoi and cuoi.t == 'Str' then
      kq[#kq] = pandoc.Str(cuoi.text .. x.text)
    else
      kq:insert(x)
    end
  end
  return kq
end

-- "**Quyền:** <vai trò>. <mô tả>" → dòng "Quyền truy cập: <vai trò>" + đoạn mô tả (nếu có)
local function quyen_truy_cap(ils)
  local cat = #ils
  for k, x in ipairs(ils) do
    if x.t == 'Str' and x.text:match('%.$') and (k == #ils or la_cach(ils[k + 1])) then
      cat = k
      break
    end
  end
  local vai_tro = pandoc.Inlines { table.unpack(ils, 1, cat) }
  local cuoi = vai_tro[#vai_tro]
  if cuoi and cuoi.t == 'Str' and cuoi.text:match('%.$') then vai_tro[#vai_tro] = pandoc.Str(cuoi.text:sub(1, -2)) end
  local kq = { kieu('QuyenTruyCap', { pandoc.Para(noi({ pandoc.Strong(chu('Quyền truy cập:')) }, vai_tro)) }) }
  local con = pandoc.Inlines { table.unpack(ils, cat + 2) }
  if #con > 0 then kq[2] = pandoc.Para(con) end
  return kq
end

local function nhan_request(b, ten_route)
  if b.t == 'CodeBlock' then return 'REQUEST BODY (JSON)' end
  if sau_nhan(b, 'Request:') then return 'REQUEST' end
  if b.t ~= 'Table' or #b.head.rows ~= 1 then return nil end
  local dau = str(b.head.rows[1].cells[1].contents)
  if not dau:match('^Trường') then return nil end
  local loai = dau:match('^Trường %((%a+)')
  if not loai then return 'REQUEST' end
  return NHAN_REQUEST[loai] or loi('route ', ten_route, ': bảng "', dau, '" không rõ loại request')
end

-- Dòng **Lỗi:** → bảng HTTP | maLoi | Điều kiện. Chữ được tách theo ( ) ; , . : để đọc cú pháp
--   mệnh đề = HTTP mã ("," mã)*, các mệnh đề cách nhau ";"; mã = `MA_LOI` [ "(" điều kiện ")" | ":" điều kiện tới ";" ]
-- Dạng đoạn: có thể có phần dẫn kết thúc bằng ":" và câu đuôi sau dấu "." cuối. Dạng danh sách: mỗi dòng 1 mệnh đề, hoặc
-- "HTTP:" kèm danh sách con các mã. Không đọc được thì trả nil (giữ nguyên chữ).
local function tach(ils)
  local kq = {}
  for _, x in ipairs(ils) do
    if x.t == 'Str' then
      local i = 1
      while i <= #x.text do
        local j = x.text:find('[();,.:]', i)
        if not j then
          kq[#kq + 1] = pandoc.Str(x.text:sub(i))
          break
        end
        if j > i then kq[#kq + 1] = pandoc.Str(x.text:sub(i, j - 1)) end
        kq[#kq + 1] = pandoc.Str(x.text:sub(j, j))
        i = j + 1
      end
    elseif x.t == 'SoftBreak' then
      kq[#kq + 1] = pandoc.Space()
    elseif x.t ~= 'RawInline' then
      kq[#kq + 1] = x
    end
  end
  return kq
end

local function la(x, ky)
  return x and x.t == 'Str' and x.text == ky
end

local function bo_cach(T, i)
  while la_cach(T[i]) do i = i + 1 end
  return i
end

local function la_ma(x)
  return x and x.t == 'Code' and x.text:match('^[A-Z][A-Z0-9_]+$')
end

local function la_http(x)
  return x and x.t == 'Str' and x.text:match('^[1-5]%d%d$')
end

local function doan_tu(T, a, b)
  return bo_cach_cuoi(gop({ table.unpack(T, bo_cach(T, a), b) }))
end

-- Vị trí k mở 1 mệnh đề mới (HTTP rồi mã)
local function mo_menh_de(T, k)
  return la_http(T[k]) and la_ma(T[bo_cach(T, k + 1)])
end

-- Đọc các mã của 1 HTTP từ vị trí i. Trả vị trí sau mệnh đề, hoặc nil. Điều kiện sau ":" chạy tới dấu ";" mở mệnh đề
-- mới hoặc tới hết
local function doc_ma(T, i, http, dong)
  while true do
    i = bo_cach(T, i)
    if not la_ma(T[i]) then return nil end
    local ma, j = T[i].text, bo_cach(T, i + 1)
    local dk = {}
    if la(T[j], '(') then
      local sau, m = 0, j
      repeat
        if la(T[m], '(') then sau = sau + 1 elseif la(T[m], ')') then sau = sau - 1 end
        m = m + 1
      until sau == 0 or m > #T
      if sau ~= 0 then return nil end
      dk, i = doan_tu(T, j + 1, m - 2), m
    elseif la(T[i + 1], ':') then
      local sau, m = 0, i + 2
      while m <= #T and not (sau == 0 and la(T[m], ';')
          and (bo_cach(T, m + 1) > #T or mo_menh_de(T, bo_cach(T, m + 1)))) do
        if la(T[m], '(') then sau = sau + 1 elseif la(T[m], ')') then sau = sau - 1 end
        m = m + 1
      end
      local het = m - 1
      if m > #T and (la(T[het], '.') or la(T[het], ';')) then het = het - 1 end
      dk, i = doan_tu(T, i + 2, het), m
    else
      i = i + 1
    end
    table.insert(dong, { http, ma, dk })
    local k = bo_cach(T, i)
    if not la(T[k], ',') then return k end
    i = k + 1
  end
end

-- Đọc các mệnh đề từ vị trí i tới hết. Trả dòng bảng và câu đuôi (có thể rỗng), hoặc nil
local function doc_menh_de(T, i, dong)
  while true do
    i = bo_cach(T, i)
    if not la_http(T[i]) then return nil end
    i = doc_ma(T, i + 1, T[i].text, dong)
    if not i then return nil end
    if i > #T then return {} end
    if la(T[i], ';') then
      if bo_cach(T, i + 1) > #T then return {} end
      i = i + 1
    elseif la(T[i], '.') then
      return doan_tu(T, i + 1, #T)
    else
      return nil
    end
  end
end

-- Ô điều kiện rỗng mang 1 dấu cách không ngắt: đoạn rỗng không nhận style Compact nên dòng bảng cao hơn
local function bang_loi(dong)
  local hang = {}
  for _, d in ipairs(dong) do
    local dk = #d[3] > 0 and pandoc.Inlines(d[3]) or pandoc.Inlines { pandoc.Str('\u{A0}') }
    table.insert(hang, { o(d[1]), o(pandoc.Inlines { pandoc.Code(d[2]) }), o(dk) })
  end
  return bang({ { 0.08, 'AlignCenter' }, { 0.38 }, { 0.54 } }, { o('HTTP'), o('maLoi'), o('Điều kiện') }, hang)
end

-- Phần dẫn trước HTTP đầu tiên phải rỗng hoặc kết thúc bằng ":"
local function tach_dan(T)
  local i = 1
  while i <= #T and not mo_menh_de(T, i) do i = i + 1 end
  if i > #T then return nil end
  local dan = doan_tu(T, 1, i - 1)
  if #dan > 0 and not (dan[#dan].t == 'Str' and dan[#dan].text:match(':$')) then return nil end
  return dan, i
end

local function doc_loi(ils, ds)
  local dong = {}
  local kq = { nhan_khoi('MÃ LỖI') }
  if not ds then
    local T = tach(ils)
    local dan, i = tach_dan(T)
    if not dan then return nil end
    local duoi = doc_menh_de(T, i, dong)
    if not duoi then return nil end
    if #dan > 0 then table.insert(kq, pandoc.Para(dan)) end
    table.insert(kq, bang_loi(dong))
    if #duoi > 0 then table.insert(kq, pandoc.Para(duoi)) end
    return kq
  end
  local dan = bo_cach_cuoi(gop(tach(ils)))
  if #dan > 0 and not (dan[#dan].t == 'Str' and dan[#dan].text:match(':$')) then return nil end
  for _, muc in ipairs(ds.content) do
    local dau = muc[1]
    if not (dau and (dau.t == 'Plain' or dau.t == 'Para')) then return nil end
    local T = tach(dau.content)
    if #muc == 1 then
      local duoi = doc_menh_de(T, 1, dong)
      if not duoi or #duoi > 0 then return nil end
    elseif #muc == 2 and muc[2].t == 'BulletList' then
      -- "HTTP:" rồi danh sách con các mã
      local i = bo_cach(T, 1)
      if not (la_http(T[i]) and la(T[i + 1], ':') and bo_cach(T, i + 2) > #T) then return nil end
      for _, con in ipairs(muc[2].content) do
        if #con ~= 1 or not (con[1].t == 'Plain' or con[1].t == 'Para') then return nil end
        local T2 = tach(con[1].content)
        local k = doc_ma(T2, 1, T[i].text, dong)
        if not k then return nil end
        if la(T2[k], ';') or la(T2[k], '.') then k = k + 1 end
        if bo_cach(T2, k) <= #T2 then return nil end
      end
    else
      return nil
    end
  end
  if #dan > 0 then table.insert(kq, pandoc.Para(dan)) end
  table.insert(kq, bang_loi(dong))
  return kq
end

-- Khối Markdown sau #### của 1 route (đã đổi "mục N") → bố cục route của tài liệu cũ: Quyền truy cập, mô tả, nhãn
-- REQUEST… trước khối request đầu tiên, RESPONSE THÀNH CÔNG — <mã>, MÃ LỖI + bảng
local function khoi_route(khoi, ten_route)
  local quyen = sau_nhan(khoi[1], 'Quyền:') or loi('route ', ten_route, ': khối đầu phải là đoạn **Quyền:**')
  local kq = pandoc.Blocks(quyen_truy_cap(quyen))
  local co_response, co_request = false, false
  local i = 2
  while i <= #khoi do
    local b = khoi[i]
    local response, req, loi_route = sau_nhan(b, 'Response:'), sau_nhan(b, 'Request:'), sau_nhan(b, 'Lỗi:')
    if response then
      co_response = true
      local ma = '200'
      if la(response[1], 'HTTP') and la_cach(response[2]) and response[3] and response[3].t == 'Str' then
        ma = response[3].text:match('^(%d%d%d)[,.]$') or loi('route ', ten_route, ': không đọc được "HTTP ',
          response[3].text, '"')
        response = pandoc.Inlines { table.unpack(response, 4) }
        while la_cach(response[1]) do response:remove(1) end
      end
      kq:insert(nhan_khoi('RESPONSE THÀNH CÔNG — ' .. (TRANG_THAI[ma] or loi('route ', ten_route, ': HTTP ', ma,
        ' chưa có tên trạng thái'))))
      if #response > 0 then kq:insert(pandoc.Para(response)) end
    elseif loi_route then
      local ds = khoi[i + 1] and khoi[i + 1].t == 'BulletList' and khoi[i + 1] or nil
      if i + (ds and 1 or 0) ~= #khoi then loi('route ', ten_route, ': **Lỗi:** phải là khối cuối') end
      local bang_ma = doc_loi(loi_route, ds)
      if bang_ma then
        kq:extend(bang_ma)
      else
        kq:insert(nhan_khoi('MÃ LỖI'))
        if #loi_route > 0 then kq:insert(pandoc.Para(loi_route)) end
        if ds then kq:insert(ds) end
      end
      i = #khoi
    else
      if not co_response and not co_request then
        local nhan = nhan_request(b, ten_route)
        if nhan then
          kq:insert(nhan_khoi(nhan))
          co_request = true
        end
      end
      if req then
        if co_response then loi('route ', ten_route, ': **Request:** sau **Response:**') end
        if #req > 0 then kq:insert(pandoc.Para(req)) end
      else
        kq:insert(b)
      end
    end
    i = i + 1
  end
  if not co_response then loi('route ', ten_route, ': thiếu đoạn **Response:**') end
  return kq
end

-- Phụ lục -----------------------------------------------------------------------------------------------------------

local PHU_LUC = { a = 'Bảng mã lỗi (maLoi)', b = 'Luồng gọi mẫu', c = 'Chạy thử và tài khoản mẫu' }

-- Mã lỗi trong code, scripts/xuat-docx-api.js truyền qua biến môi trường: mảng { ma, http, thongDiep, gateway }
local function doc_ma_loi()
  local s = os.getenv('MA_LOI_JSON') or loi('thiếu biến môi trường MA_LOI_JSON (xuất bằng scripts/xuat-docx-api.js)')
  return pandoc.json.decode(s, false)
end

-- Các mã của `ds` có mặt dưới dạng chữ code trong `blocks`, theo thứ tự của `ds`
local function ma_duoc_nhac(blocks, ds)
  local co, kq = {}, {}
  pandoc.Blocks(blocks):walk { Code = function(c) co[c.text] = true end }
  for _, d in ipairs(ds) do
    if co[d.ma] then table.insert(kq, d) end
  end
  return kq
end

local function phu_luc_a(ma, muc_ngon_ngu)
  if #ma == 0 then loi('tài liệu không nhắc mã lỗi nào có trong code') end
  local gw, dong = {}, {}
  for _, d in ipairs(ma) do
    if d.gateway then table.insert(gw, '`' .. d.ma .. '`') end
    table.insert(dong, { o(pandoc.Inlines { pandoc.Code(d.ma) }), o(('%d'):format(d.http)), o(d.thongDiep) })
  end
  local dan = 'Mọi mã lỗi mà tài liệu nhắc tới, theo thứ tự khai báo trong code. Thông điệp dưới đây là mặc định; nhiều'
    .. ' route trả thông điệp cụ thể hơn trong cùng mã, nên frontend xử lý theo `maLoi`, không theo thông điệp. Gửi'
    .. ' `Accept-Language: en` (mục ' .. muc_ngon_ngu .. ') thì nhận thông điệp tiếng Anh; `maLoi` và HTTP không đổi.'
  if #gw > 0 then
    dan = dan .. ' Mã do api-gateway tự trả (request chưa tới service): ' .. table.concat(gw, ', ') .. '.'
  end
  local kq = pandoc.Blocks { tieu_de(1, 'Phụ lục A.', chu(PHU_LUC.a), 'phu-luc-a') }
  kq:extend(pandoc.read(dan, 'gfm').blocks)
  kq:insert(bang({ { 0.38 }, { 0.08, 'AlignCenter' }, { 0.54 } },
    { o('maLoi'), o('HTTP'), o('Thông điệp mặc định (tiếng Việt)') }, dong))
  return kq
end

-- Lấy nhãn **… luồng chính** và danh sách đánh số ngay sau nó ra khỏi `blocks`; trả các mục của danh sách
local function tach_luong(blocks, ten_muc)
  for i, b in ipairs(blocks) do
    if b.t == 'Para' and #b.content == 1 and b.content[1].t == 'Strong' and str(b.content[1]):match('luồng chính$') then
      local ds = blocks[i + 1]
      if not (ds and ds.t == 'OrderedList') then loi('mục ', ten_muc, ': sau nhãn "', str(b), '" cần danh sách đánh số') end
      table.remove(blocks, i + 1)
      table.remove(blocks, i)
      return ds.content
    end
  end
  loi('mục ', ten_muc, ' không có nhãn "**… luồng chính**"')
end

-- 1 luồng → tiêu đề (chữ đậm đầu, bỏ ":" cuối) + thân. Chữ đậm chỉ bỏ khỏi thân khi kết thúc bằng ":" (vd "**Ngày khám:**",
-- "**Mất link phiếu khám**:"); còn lại câu giữ nguyên dưới tiêu đề.
local function luong(item)
  local dau = item[1]
  local dam = dau and (dau.t == 'Plain' or dau.t == 'Para') and dau.content[1]
  if not (dam and dam.t == 'Strong') then loi('luồng không mở đầu bằng chữ đậm: ', str(dau and { dau } or {})) end
  local ten = pandoc.Inlines(dam.content):clone()
  local cuoi = ten[#ten]
  local hai_cham = cuoi and cuoi.t == 'Str' and cuoi.text:match(':$')
  if hai_cham then
    cuoi.text = cuoi.text:sub(1, -2)
    if cuoi.text == '' then ten:remove(#ten) end
  end
  local than = pandoc.Blocks(item):clone()
  local ils = than[1].content
  local sau = ils[2]
  if hai_cham or (sau and sau.t == 'Str' and sau.text:match('^:')) then
    ils:remove(1)
    if not hai_cham then
      ils[1].text = ils[1].text:sub(2)
      if ils[1].text == '' then ils:remove(1) end
    end
    while la_cach(ils[1]) do ils:remove(1) end
    than[1].content = ils
    if #ils == 0 then than:remove(1) end
  end
  return bo_cach_cuoi(ten), than
end

function Pandoc(doc)
  local ma_nhac = ma_duoc_nhac(doc.blocks, doc_ma_loi())
  local cay, route = doc_cay(doc.blocks)
  local vai_tro = cay.h1 and cay.h1:match(' — (.+)$') or loi('# cần dạng "Tên — vai trò"')
  local p1, p2 = cay.phan[1], cay.phan[2]
  if not (p1 and p1.so == '1' and p2 and p2.so == '2' and #cay.phan >= 3) then
    loi('cần ## 1 (quy ước), ## 2 (route dùng chung) và ít nhất 1 phần vai trò')
  end

  -- Bảng đổi số mục cũ → { đầu, cuối } mới
  local doi, so_h1, quy_uoc, muc_mo_hinh = {}, 4, {}, nil
  for _, m in ipairs(p1.muc) do
    local co_mo_hinh = false
    for _, b in ipairs(m.dau) do co_mo_hinh = co_mo_hinh or la_mo_hinh(b) end
    if #m.con > 0 then loi('mục ', m.so, ' không được có ####') end
    if co_mo_hinh then
      muc_mo_hinh = m
      doi[m.so] = { '4', '4' }
    else
      if muc_mo_hinh then loi('mục mô hình ', muc_mo_hinh.so, ' phải là mục cuối của ## 1') end
      table.insert(quy_uoc, m)
      doi[m.so] = { '3.' .. #quy_uoc, '3.' .. #quy_uoc }
    end
  end
  muc_mo_hinh = muc_mo_hinh or loi('## 1 không có mục mô hình (đoạn **`Ten`**)')
  doi['1'] = { '3', '4' }
  local nhom = {}
  local function them_nhom(p, ds)
    if #ds == 0 then loi('## ', p.so, ' không có mục route nào') end
    local dau = so_h1 + 1
    for _, m in ipairs(ds) do
      so_h1 = so_h1 + 1
      doi[m.so] = { tostring(so_h1), tostring(so_h1) }
      table.insert(nhom, { m = m, so = so_h1, phan = p, dau_phan = m == ds[1] })
      for _, c in ipairs(m.con) do
        if not c.pt then loi('mục ', m.so, ' chỉ được có route, gặp "', str(c.h.content), '"') end
      end
    end
    doi[p.so] = { tostring(dau), tostring(so_h1) }
  end
  them_nhom(p2, p2.muc)
  local phan_vai_tro = {}
  for k = 3, #cay.phan do
    local p = cay.phan[k]
    local x1, x2 = p.muc[1], p.muc[2]
    if not (x1 and x2) then loi('## ', p.so, ' cần mục tổng quan và mục mô hình') end
    if #x1.con > 0 then loi('mục ', x1.so, ' không được có ####') end
    for _, c in ipairs(x2.con) do
      if c.pt then loi('mục ', x2.so, ' chỉ được có mô hình, gặp route ', c.pt, ' ', c.dd) end
    end
    local so = '3.' .. (#quy_uoc + #phan_vai_tro + 1)
    doi[x1.so], doi[x2.so] = { so, so }, { '4', '4' }
    table.insert(phan_vai_tro, { p = p, x1 = x1, x2 = x2, so = so })
    them_nhom(p, { table.unpack(p.muc, 3) })
  end

  local bo_doi = bo_doi_muc(doi)
  local function md(blocks)
    return pandoc.Blocks(blocks):walk(bo_doi)
  end
  local function ten(h)
    local ils = pandoc.Inlines(h.content):walk(bo_code)
    if ils[1] and ils[1].t == 'Str' and ils[1].text:match('^%d') then ils:remove(1) end
    if la_cach(ils[1]) then ils:remove(1) end
    return pandoc.Inlines(ils):walk(bo_doi)
  end

  local id_da_dung = {}
  doc.blocks:walk { Header = function(h) id_da_dung[h.identifier] = true end }

  -- Route theo thứ tự xuất hiện, kèm số mới và thông tin từ bảng tổng quan
  local ds_route, theo_phan = {}, {}
  for _, n in ipairs(nhom) do
    for k, c in ipairs(n.m.con) do
      local tt = route[c.pt .. ' ' .. c.dd] or loi('route ', c.pt, ' ', c.dd, ' không có dòng trong bảng "Tất cả route'
        .. ' của mục ', n.phan.so, '"')
      c.so, c.quyen, c.muc_dich = n.so .. '.' .. k, tt.quyen, pandoc.Inlines(tt.muc_dich):walk(bo_code)
      table.insert(ds_route, c)
      theo_phan[n.phan] = (theo_phan[n.phan] or 0) + 1
    end
  end

  -- Bìa
  local m11 = quy_uoc[1]
  local dia_chi, dich_vu = nil, {}
  pandoc.Blocks(m11.dau):walk { Code = function(c) dia_chi = dia_chi or (c.text:match('^https?://') and c.text) end }
  for _, b in ipairs(m11.dau) do
    if dau_bang(b, 'Tiền tố', 'Service') then
      for _, d in ipairs(dong_than(b)) do
        local dv = { ten = str(d.cells[2].contents), so = 0, tien_to = {} }
        pandoc.Blocks(d.cells[1].contents):walk { Code = function(c)
          table.insert(dv.tien_to, (c.text:gsub('/%*%*$', '')))
        end }
        table.insert(dich_vu, dv)
      end
    end
  end
  if not dia_chi or #dich_vu == 0 then loi('mục ', m11.so, ' cần địa chỉ gateway (http…) và bảng Tiền tố / Service') end
  for _, c in ipairs(ds_route) do
    local thay = false
    for _, dv in ipairs(dich_vu) do
      for _, tt in ipairs(dv.tien_to) do
        if not thay and (c.dd == tt or c.dd:sub(1, #tt + 1) == tt .. '/') then dv.so, thay = dv.so + 1, true end
      end
    end
    if not thay then loi('route ', c.pt, ' ', c.dd, ' không thuộc tiền tố nào ở mục ', m11.so) end
  end
  local phien_ban
  for _, b in ipairs(cay.dau) do
    if dau_bang(b, 'Phiên bản', 'Nội dung') then
      local dong = dong_than(b)
      phien_ban = #dong > 0 and noi_dung_o(dong[#dong].cells[1])
    end
  end
  phien_ban = phien_ban or loi('đầu tài liệu cần bảng | Phiên bản | Nội dung |')
  local theo_ten_phan, theo_dv = {}, {}
  for k = 2, #cay.phan do
    local p = cay.phan[k]
    table.insert(theo_ten_phan, str(ten(p.h)) .. ': ' .. (theo_phan[p] or 0))
  end
  for _, dv in ipairs(dich_vu) do
    if dv.so > 0 then table.insert(theo_dv, dv.ten .. ': ' .. dv.so) end
  end
  local kq = pandoc.Blocks {
    kieu('NhanBia', { pandoc.Para(chu('eClinic Backend')) }),
    kieu('Title', { pandoc.Para(chu('Tài liệu API')) }),
    kieu('Subtitle', { pandoc.Para(chu(vai_tro)) }),
    bang({ { 0.22 }, { 0.78 } }, nil, {
      { o(pandoc.Inlines { pandoc.Strong(chu('Phiên bản')) }), o(phien_ban) },
      { o(pandoc.Inlines { pandoc.Strong(chu('Phạm vi')) }), pandoc.Cell(pandoc.Blocks {
        pandoc.Plain(chu(#ds_route .. ' route. Theo phần: ' .. table.concat(theo_ten_phan, '; ') .. '.')),
        pandoc.Plain(chu('Theo service: ' .. table.concat(theo_dv, '; ') .. '.')) }) },
      { o(pandoc.Inlines { pandoc.Strong(chu('Căn cứ')) }), o(CAN_CU) },
      { o(pandoc.Inlines { pandoc.Strong(chu('Địa chỉ gọi API')) }),
        o(noi({ pandoc.Code(dia_chi) }, '(api-gateway); cổng từng service: mục 3.1')) },
    }),
  }

  -- Phần chuyển xuống phụ lục: B là các luồng ở mục tổng quan của từng phần vai trò, C là phần từ nhãn
  -- **Chạy thử và tài khoản mẫu** tới hết mục 1.1
  local luong_b = {}
  for _, v in ipairs(phan_vai_tro) do
    for _, item in ipairs(tach_luong(v.x1.dau, v.x1.so)) do table.insert(luong_b, item) end
  end
  local phan_c
  for i, b in ipairs(m11.dau) do
    if la_nhan(b, PHU_LUC.c) then
      phan_c = pandoc.Blocks { table.unpack(m11.dau, i + 1) }
      for _ = i, #m11.dau do table.remove(m11.dau) end
      break
    end
  end
  if not phan_c or #phan_c == 0 then loi('mục ', m11.so, ' cần nhãn "**', PHU_LUC.c, '**" và nội dung sau nó') end
  local muc_ngon_ngu
  for _, m in ipairs(quy_uoc) do
    if str(m.h.content):match('Ngôn ngữ') then muc_ngon_ngu = doi[m.so][1] end
  end
  muc_ngon_ngu = muc_ngon_ngu or loi('## 1 không có mục "Ngôn ngữ" (Phụ lục A trỏ tới)')

  -- 1. Giới thiệu và phạm vi
  local pham_vi = { 'Bố cục: mục 2 liệt kê mọi route (bấm đường dẫn để tới route); mục 3 là quy ước chung và tổng quan'
    .. ' từng phần; mục 4 là các mô hình dữ liệu dùng ở nhiều route' }
  for k = 2, #cay.phan do
    local p = cay.phan[k]
    local d = doi[p.so]
    table.insert(pham_vi, 'mục ' .. d[1] .. (d[1] == d[2] and '' or GACH .. d[2]) .. ': ' .. str(ten(p.h)))
  end
  for _, k in ipairs({ 'a', 'b', 'c' }) do
    table.insert(pham_vi, 'Phụ lục ' .. k:upper() .. ': ' .. PHU_LUC[k]:gsub(' %(maLoi%)$', ''):lower())
  end
  kq:insert(tieu_de(1, '1.', chu('Giới thiệu và phạm vi'), 'gioi-thieu-va-pham-vi'))
  local dau = md(cay.dau)
  local cho = #dau + 1
  for i, b in ipairs(dau) do
    if dau_bang(b, 'Phiên bản', 'Nội dung') then cho = la_nhan(dau[i - 1], 'Lịch sử phiên bản') and i - 1 or i end
  end
  dau:insert(cho, pandoc.Para(chu(table.concat(pham_vi, '; ') .. '.')))
  kq:extend(dau)

  -- 2. Danh sách route
  kq:insert(tieu_de(1, '2.', chu('Danh sách route'), 'danh-sach-route'))
  local dong = {}
  for _, c in ipairs(ds_route) do
    table.insert(dong, { o(c.so), o(pandoc.Inlines { huy_hieu(c.pt) }),
      o(pandoc.Inlines { pandoc.Link(pandoc.Inlines { pandoc.Code(c.dd) }, '#' .. c.h.identifier) }),
      o(c.quyen), o(c.muc_dich) })
  end
  kq:insert(bang({ { 0.07 }, { 0.10, 'AlignCenter' }, { 0.40 }, { 0.19 }, { 0.24 } },
    { o('Mục'), o('Method'), o('Đường dẫn'), o('Quyền'), o('Chức năng') }, dong))

  -- 3. Quy ước chung
  kq:insert(tieu_de(1, '3.', ten(p1.h), p1.h.identifier))
  kq:extend(md(p1.dau))
  for k, m in ipairs(quy_uoc) do
    kq:insert(tieu_de(2, '3.' .. k, ten(m.h), m.h.identifier))
    kq:extend(md(m.dau))
  end
  for _, v in ipairs(phan_vai_tro) do
    local h = tieu_de(2, v.so, noi('Tổng quan:', ten(v.p.h)), v.x1.h.identifier)
    kq:insert(h)
    kq:extend(md(v.p.dau))
    kq:extend(md(v.x1.dau))
    if #v.x2.dau > 0 then
      kq:insert(pandoc.Para { moc(v.x2.h.identifier), pandoc.Strong(chu('Enum của phần này')) })
      kq:extend(md(v.x2.dau))
    else
      h.content:insert(moc(v.x2.h.identifier))
    end
  end

  -- 4. Mô hình dữ liệu
  local ten_4 = chu('Mô hình dữ liệu')
  ten_4:insert(moc(muc_mo_hinh.h.identifier))
  kq:insert(tieu_de(1, '4.', ten_4, 'mo-hinh-du-lieu'))
  local so_mo_hinh = 0
  local function mo_hinh(ils, id)
    so_mo_hinh = so_mo_hinh + 1
    kq:insert(tieu_de(2, '4.' .. so_mo_hinh, ils, id))
  end
  for _, b in ipairs(md(muc_mo_hinh.dau)) do
    if la_mo_hinh(b) then
      local ma = b.content[1].content[1].text
      local sau = pandoc.Inlines { table.unpack(b.content, 2) }
      local id = ma:lower()
      if id_da_dung[id] then id = '' end
      id_da_dung[id] = true
      mo_hinh(noi(chu(ma), sau), id)
    else
      kq:insert(b)
    end
  end
  for _, v in ipairs(phan_vai_tro) do
    for _, c in ipairs(v.x2.con) do
      mo_hinh(ten(c.h), c.h.identifier)
      kq:extend(md(c.khoi))
    end
  end

  -- 5 trở đi: nhóm route
  for _, n in ipairs(nhom) do
    local ils = ten(n.m.h)
    if n.dau_phan then ils:insert(moc(n.phan.h.identifier)) end
    kq:insert(tieu_de(1, n.so .. '.', ils, n.m.h.identifier))
    -- Phần đầu của ## vai trò đã vào mục tổng quan 3.x; chỉ ## 2 không có mục tổng quan
    if n.dau_phan and n.phan == p2 then kq:extend(md(n.phan.dau)) end
    kq:extend(md(n.m.dau))
    for _, c in ipairs(n.m.con) do
      kq:insert(tieu_de(2, c.so, c.muc_dich, c.h.identifier))
      kq:insert(kieu('ThanhRoute', { pandoc.Para { huy_hieu(c.pt), pandoc.Space(), pandoc.Str(c.dd) } }))
      kq:extend(khoi_route(md(c.khoi), c.pt .. ' ' .. c.dd))
    end
  end

  -- Phụ lục A–C
  kq:extend(phu_luc_a(ma_nhac, muc_ngon_ngu))
  kq:insert(tieu_de(1, 'Phụ lục B.', chu(PHU_LUC.b), 'phu-luc-b'))
  for k, item in ipairs(luong_b) do
    local ten_luong, than = luong(item)
    kq:insert(tieu_de(2, 'B.' .. k, ten_luong))
    kq:extend(md(than))
  end
  kq:insert(tieu_de(1, 'Phụ lục C.', chu(PHU_LUC.c), 'phu-luc-c'))
  kq:extend(md(phan_c))

  doc.blocks = kq
  doc.meta.title = nil
  return doc
end
