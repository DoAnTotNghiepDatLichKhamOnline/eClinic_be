// Mô hình ngôn ngữ GIẢ cho smoke phase 8 (DOANTOTNGH-9), gồm cả mọi luật của phase 7 (temp/d9-p7-mo-hinh-gia.js) để
// smoke phase 7 chạy lại được. Nói định dạng "chat completions" của OpenAI, ghi lại mọi request, trả lời theo luật dựa
// trên câu hỏi CUỐI của người dùng và số kết quả tra cứu (role: tool) đã nhận sau câu hỏi đó:
//   #cham / #429 / #500 / #rong / #ngoai / #ngoai2 / #ngoai3      như phase 7
//   #bacsi <x>            xin tim_bac_si(idChuyenKhoa = x) rồi trả "DA TRA CUU: <các kết quả>"
//   #lich <id> [ngay]     xin xem_lich_bac_si(idBacSi, tuNgay) rồi trả "DA TRA CUU: ..."
//   #haibuoc <ck> <bs>    lượt 1 xin tim_bac_si, lượt 2 xin xem_lich_bac_si, rồi trả lời
//   #songsong <ck> <bs>   1 lượt xin cả 2 hàm
//   #mai                  lần nào cũng xin tra cứu, kể cả khi tool_choice = none
//   #la                   xin 1 hàm không tồn tại          #thamsohong   tham số không phải JSON
//   #ngoaisau <ck>        xin tim_bac_si rồi trả [NGOAI_PHAM_VI]
//   #cham2                đợi 2 giây rồi xin tra cứu, lần nào cũng vậy
//   còn lại               "TRA LOI GIA: <câu hỏi>"
// Lời gọi công cụ có thêm trường lạ extra_content (như chữ ký suy luận của Gemini): server phải gửi lại nguyên vẹn.
// GET /ghi: các request đã nhận ({ authorization, body });  DELETE /ghi: xoá.
const http = require('http');
const ghi = [];
const guiJson = (res, ma, duLieu) => { res.writeHead(ma, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(duLieu)); };
const traLoi = (noiDung) => ({ id: 'gia', object: 'chat.completion', choices: [{ index: 0, message: { role: 'assistant', content: noiDung }, finish_reason: 'stop' }] });
const xin = (...cacLoiGoi) => ({
  id: 'gia', object: 'chat.completion', choices: [{
    index: 0, finish_reason: 'tool_calls', message: {
      role: 'assistant', content: null,
      tool_calls: cacLoiGoi.map(([ten, thamSo], i) => ({
        id: 'goi-' + (i + 1), type: 'function', extra_content: { google: { thought_signature: 'chu-ky-gia-' + (i + 1) } },
        function: { name: ten, arguments: typeof thamSo === 'string' ? thamSo : JSON.stringify(thamSo) },
      })),
    },
  }],
});
const so = (x) => (/^\d+$/.test(x || '') ? Number(x) : x);
const timBacSi = (ck) => ['tim_bac_si', { idChuyenKhoa: so(ck) }];
const xemLich = (bs, ngay) => ['xem_lich_bac_si', ngay ? { idBacSi: so(bs), tuNgay: ngay } : { idBacSi: so(bs) }];

http.createServer((req, res) => {
  if (req.url === '/ghi' && req.method === 'GET') return guiJson(res, 200, ghi);
  if (req.url === '/ghi' && req.method === 'DELETE') { ghi.length = 0; return guiJson(res, 200, {}); }
  if (req.url !== '/v1/chat/completions' || req.method !== 'POST') return guiJson(res, 404, { error: { message: 'not found' } });
  let than = '';
  req.on('data', (d) => { than += d; });
  req.on('end', () => {
    let body = {};
    try { body = JSON.parse(than); } catch { /* giữ {} */ }
    ghi.push({ authorization: req.headers.authorization || null, body });
    const tinNhan = body.messages || [];
    let viTriCuoi = -1;
    tinNhan.forEach((m, i) => { if (m.role === 'user') viTriCuoi = i; });
    const cuoi = viTriCuoi < 0 ? '' : tinNhan[viTriCuoi].content || '';
    const ketQua = tinNhan.slice(viTriCuoi + 1).filter((m) => m.role === 'tool').map((m) => m.content);
    const daTraCuu = () => guiJson(res, 200, traLoi('DA TRA CUU: ' + ketQua.join(' || ')));
    const p = cuoi.trim().split(/\s+/);

    if (cuoi.includes('#cham2')) return setTimeout(() => guiJson(res, 200, xin(timBacSi('1'))), 2000);
    if (cuoi.includes('#cham')) return setTimeout(() => guiJson(res, 200, traLoi('TRA LOI CHAM')), 8000);
    if (cuoi.includes('#429')) return guiJson(res, 429, { error: { message: 'quota exceeded', code: 429 } });
    if (cuoi.includes('#500')) return guiJson(res, 500, { error: { message: 'internal', code: 500 } });
    if (cuoi.includes('#rong')) return guiJson(res, 200, traLoi(''));
    if (cuoi.includes('#ngoaisau')) return guiJson(res, 200, ketQua.length ? traLoi('[NGOAI_PHAM_VI]') : xin(timBacSi(p[1])));
    if (cuoi.includes('#ngoai3')) return guiJson(res, 200, traLoi('ngoai_pham_vi'));
    if (cuoi.includes('#ngoai2')) return guiJson(res, 200, traLoi('Xin loi ban. [NGOAI_PHAM_VI] Toi khong giup duoc viec nay.'));
    if (cuoi.includes('#ngoai')) return guiJson(res, 200, traLoi('[NGOAI_PHAM_VI]'));
    if (cuoi.includes('#mai')) return guiJson(res, 200, xin(timBacSi('1')));
    if (cuoi.startsWith('#bacsi')) return ketQua.length ? daTraCuu() : guiJson(res, 200, xin(timBacSi(p[1])));
    if (cuoi.startsWith('#lich')) return ketQua.length ? daTraCuu() : guiJson(res, 200, xin(xemLich(p[1], p[2])));
    if (cuoi.startsWith('#haibuoc')) {
      if (ketQua.length === 0) return guiJson(res, 200, xin(timBacSi(p[1])));
      if (ketQua.length === 1) return guiJson(res, 200, xin(xemLich(p[2])));
      return daTraCuu();
    }
    if (cuoi.startsWith('#songsong')) return ketQua.length ? daTraCuu() : guiJson(res, 200, xin(timBacSi(p[1]), xemLich(p[2])));
    if (cuoi.startsWith('#la')) return ketQua.length ? daTraCuu() : guiJson(res, 200, xin(['xoa_du_lieu', { bang: 'tai_khoan' }]));
    if (cuoi.startsWith('#thamsohong')) return ketQua.length ? daTraCuu() : guiJson(res, 200, xin(['tim_bac_si', 'khong phai json']));
    return guiJson(res, 200, traLoi('TRA LOI GIA: ' + cuoi));
  });
}).listen(18999, '0.0.0.0', () => console.log('mo hinh gia: 18999'));
