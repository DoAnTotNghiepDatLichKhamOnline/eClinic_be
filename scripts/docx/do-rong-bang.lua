-- Bộ lọc pandoc cho scripts/xuat-docx-api.js. Bảng pipe của Markdown không mang độ rộng, nên mặc định pandoc để bảng
-- hẹp và chia cột đều. Ở đây mỗi cột được:
--   - tối thiểu đủ rộng cho từ dài nhất (mã lỗi, tên trường không có dấu cách) để không bị ngắt giữa chữ;
--   - phần bề ngang còn lại chia theo chữ dài nhất của cột (cắt ở DAI_NHAT).
-- Đơn vị là số ký tự; BE_NGANG là số ký tự vừa 1 dòng chữ ở trang A4 lề 2 cm. Chữ hoa, chữ số và "_" rộng hơn chữ
-- thường (QUAN_TRI_VIEN, mã lỗi) nên tính CHU_RONG ký tự.
-- Chữ code ở dòng tiêu đề đổi thành chữ thường để không mất màu trên nền tiêu đề.
-- Bảng có class co-dinh (do bo-cuc.lua dựng, đã có độ rộng cột) giữ nguyên.
local BE_NGANG, NGAN_NHAT, DAI_NHAT, TU_DAI_NHAT, CHU_RONG = 95, 4, 60, 32, 1.3

local function do_dai(chu)
  local rong = select(2, chu:gsub('[%u%d_]', ''))
  return (utf8.len(chu) or #chu) + rong * (CHU_RONG - 1)
end

local function quet(dong, dai, tu)
  for _, d in ipairs(dong) do
    for i, o in ipairs(d.cells) do
      if dai[i] then
        local chu = pandoc.utils.stringify(o.contents)
        dai[i] = math.max(dai[i], do_dai(chu))
        for t in chu:gmatch('%S+') do tu[i] = math.max(tu[i], do_dai(t)) end
      end
    end
  end
end

local bo_code = { Code = function(c) return pandoc.Str(c.text) end }

function Table(bang)
  if bang.classes:includes('co-dinh') then return nil end
  local dai, tu = {}, {}
  for i = 1, #bang.colspecs do dai[i], tu[i] = 0, 0 end
  quet(bang.head.rows, dai, tu)
  for _, than in ipairs(bang.bodies) do
    quet(than.head, dai, tu)
    quet(than.body, dai, tu)
  end
  local rong, tong_min, tong_them = {}, 0, 0
  for i = 1, #dai do
    rong[i] = math.max(NGAN_NHAT, math.min(tu[i], TU_DAI_NHAT)) + 3
    dai[i] = math.max(0, math.min(dai[i], DAI_NHAT) + 3 - rong[i])
    tong_min, tong_them = tong_min + rong[i], tong_them + dai[i]
  end
  local con_lai = math.max(0, BE_NGANG - tong_min)
  local tong = 0
  for i = 1, #rong do
    if tong_them > 0 then rong[i] = rong[i] + con_lai * dai[i] / tong_them end
    tong = tong + rong[i]
  end
  for i, cot in ipairs(bang.colspecs) do
    bang.colspecs[i] = { cot[1], rong[i] / tong }
  end
  for _, d in ipairs(bang.head.rows) do
    for _, o in ipairs(d.cells) do
      o.contents = pandoc.Blocks(o.contents):walk(bo_code)
    end
  end
  return bang
end
