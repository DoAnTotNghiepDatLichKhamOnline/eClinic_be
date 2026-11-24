#!/usr/bin/env node
/*
 * Tìm ứng viên chân dung bác sĩ mẫu trên Pixabay (DOANTOTNGH-28 phase 4). Chỉ tìm và lọc, không tải ảnh, không sửa
 * nguon.json: người chạy xem bảng ảnh (đánh số theo thứ tự trong file kết quả), tự xác định giới tính, rồi chọn tay.
 *
 *   node scripts/anh-mau/tim-pixabay.js <file-ket-qua.json>
 *
 * Đọc PIXABAY_API_KEY từ .env ở thư mục gốc (không bao giờ in ra). Pixabay có ít ảnh bác sĩ thật (phần lớn ảnh y tế là
 * ảnh AI, ảnh COVID / khẩu trang, hoặc không có tag giới tính), nên lọc rộng rồi xem bằng mắt:
 *  - bỏ ảnh AI (isAiGenerated), ảnh chất lượng thấp (isLowQuality), cạnh ngắn < 1200 px;
 *  - phải có tag nghề y; bỏ tag nhóm / trẻ em / khẩu trang / COVID;
 *  - gioiTinhTag = giới tính theo tag nếu có (chỉ để tham khảo, giới tính thật xác định lúc xem ảnh).
 * Ảnh Pixabay không được hotlink lâu dài: nguon.json lưu 'pb:<id>', tai-len.js hỏi lại API lấy URL lúc tải.
 */
const fs = require('fs');
const path = require('path');

const GOC = path.resolve(__dirname, '..', '..');

const TRUY_VAN = ['doctor', 'doctor portrait', 'female doctor', 'male doctor', 'woman doctor', 'man doctor',
    'physician', 'surgeon', 'dentist', 'stethoscope', 'white coat', 'lab coat', 'scrubs', 'nurse', 'medical student',
    'pharmacist', 'healthcare worker', 'medical professional'];
const TU_NU = ['woman', 'female', 'girl', 'lady', 'woman doctor', 'female doctor', 'businesswoman'];
const TU_NAM = ['man', 'male', 'boy', 'gentleman', 'guy', 'male doctor', 'businessman'];
const TU_NGHE_Y = ['doctor', 'physician', 'medical', 'medicine', 'stethoscope', 'healthcare', 'hospital', 'clinic',
    'surgeon', 'white coat', 'lab coat', 'scrubs', 'nurse', 'dentist', 'pharmacist', 'woman doctor', 'male doctor',
    'female doctor'];
const TU_LOAI = ['women', 'men', 'group', 'team', 'teamwork', 'colleagues', 'doctors', 'nurses', 'couple', 'family',
    'child', 'children', 'kid', 'kids', 'baby', 'crowd', 'mask', 'face mask', 'surgical mask', 'protective mask',
    'covid', 'covid-19', 'coronavirus', 'virus', 'pandemic', 'skeleton', 'toy', 'doll', 'cartoon', 'illustration'];

function docKhoa() {
    for (const dong of fs.readFileSync(path.join(GOC, '.env'), 'utf8').split(/\r?\n/)) {
        const m = dong.match(/^\s*PIXABAY_API_KEY\s*=\s*(.*?)\s*$/);
        if (m && m[1]) return m[1].replace(/^["']|["']$/g, '');
    }
    throw new Error('Thiếu PIXABAY_API_KEY trong .env');
}

const ngu = (ms) => new Promise((r) => setTimeout(r, ms));

/** 1 trang kết quả; trang vượt quá 500 kết quả đầu trả 400 -> coi như hết. */
async function tim(khoa, q, trang) {
    const url = 'https://pixabay.com/api/?' + new URLSearchParams({
        key: khoa, q, image_type: 'photo', safesearch: 'true', per_page: '200', page: String(trang),
    });
    const res = await fetch(url, { signal: AbortSignal.timeout(30000) });
    if (res.status === 400) return { hits: [], conLai: res.headers.get('x-ratelimit-remaining') };
    if (!res.ok) throw new Error(`Pixabay HTTP ${res.status} (${q}, trang ${trang})`);
    return { hits: (await res.json()).hits ?? [], conLai: res.headers.get('x-ratelimit-remaining') };
}

function loc(h) {
    const tags = h.tags.toLowerCase().split(',').map((t) => t.trim());
    const co = (ds) => tags.some((t) => ds.includes(t));
    if (h.isAiGenerated || h.isLowQuality || h.type !== 'photo' || Math.min(h.imageWidth, h.imageHeight) < 1200
        || !co(TU_NGHE_Y) || co(TU_LOAI)) {
        return null;
    }
    const nu = co(TU_NU);
    const nam = co(TU_NAM);
    return nu === nam ? '?' : nu ? 'NU' : 'NAM';
}

async function main() {
    const [fileKetQua] = process.argv.slice(2);
    if (!fileKetQua) throw new Error('Cách dùng: node tim-pixabay.js <file-ket-qua.json>');
    const khoa = docKhoa();
    const daCo = new Map();
    let conLai = null;
    for (const q of TRUY_VAN) {
        for (let trang = 1; trang <= 3; trang++) {
            const r = await tim(khoa, q, trang);
            conLai = r.conLai;
            for (const h of r.hits) {
                const gioiTinhTag = daCo.has(h.id) ? null : loc(h);
                if (gioiTinhTag) {
                    daCo.set(h.id, { id: h.id, gioiTinhTag, tacGia: h.user, idTacGia: h.user_id, trangNguon: h.pageURL,
                        xemTruoc: h.webformatURL, rong: h.imageWidth, cao: h.imageHeight, tags: h.tags });
                }
            }
            await ngu(700);
            if (r.hits.length < 200) break;
        }
    }
    // Cùng tác giả đứng cạnh nhau để dễ thấy ảnh trùng người mẫu trên bảng ảnh
    const ds = [...daCo.values()].sort((a, b) => a.idTacGia - b.idTacGia || a.id - b.id);
    fs.writeFileSync(fileKetQua, JSON.stringify(ds, null, 2));
    const dem = ds.reduce((m, x) => ({ ...m, [x.gioiTinhTag]: (m[x.gioiTinhTag] ?? 0) + 1 }), {});
    console.log(`${ds.length} ứng viên (theo tag: ${JSON.stringify(dem)}), ${new Set(ds.map((x) => x.idTacGia)).size}`
        + ` tác giả; còn ${conLai} request trong phút này`);
}

main().catch((e) => { console.error(e.message); process.exit(1); });
