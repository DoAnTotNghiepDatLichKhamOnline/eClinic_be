// Mô hình ngôn ngữ GIẢ cho smoke phase 7 (DOANTOTNGH-9): nói định dạng "chat completions" của OpenAI, ghi lại mọi request
// nhận được và trả lời theo luật dựa trên câu hỏi cuối của người dùng:
//   #cham   -> đợi 8 giây rồi mới trả lời        #429 / #500 -> trả lỗi HTTP đó        #rong -> content rỗng
//   #ngoai  -> đúng dấu hiệu [NGOAI_PHAM_VI]      #ngoai2 -> dấu hiệu lẫn trong câu     #ngoai3 -> dấu hiệu thiếu ngoặc, chữ thường
//   còn lại -> "TRA LOI GIA: <câu hỏi>"
// GET /ghi: các request đã nhận ({ authorization, body });  DELETE /ghi: xoá.
// Chạy trong container trên mạng của stack smoke (temp/d9-p7-run.sh), cổng 18999.
const http = require('http');
const ghi = [];
const guiJson = (res, ma, duLieu) => { res.writeHead(ma, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(duLieu)); };
const traLoi = (noiDung) => ({ id: 'gia', object: 'chat.completion', choices: [{ index: 0, message: { role: 'assistant', content: noiDung }, finish_reason: 'stop' }] });

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
    const cuoi = ((body.messages || []).filter((m) => m.role === 'user').pop() || {}).content || '';
    if (cuoi.includes('#cham')) return setTimeout(() => guiJson(res, 200, traLoi('TRA LOI CHAM')), 8000);
    if (cuoi.includes('#429')) return guiJson(res, 429, { error: { message: 'quota exceeded', code: 429 } });
    if (cuoi.includes('#500')) return guiJson(res, 500, { error: { message: 'internal', code: 500 } });
    if (cuoi.includes('#rong')) return guiJson(res, 200, traLoi(''));
    if (cuoi.includes('#ngoai3')) return guiJson(res, 200, traLoi('ngoai_pham_vi'));
    if (cuoi.includes('#ngoai2')) return guiJson(res, 200, traLoi('Xin loi ban. [NGOAI_PHAM_VI] Toi khong giup duoc viec nay.'));
    if (cuoi.includes('#ngoai')) return guiJson(res, 200, traLoi('[NGOAI_PHAM_VI]'));
    return guiJson(res, 200, traLoi('TRA LOI GIA: ' + cuoi));
  });
}).listen(18999, '0.0.0.0', () => console.log('mo hinh gia: 18999'));
