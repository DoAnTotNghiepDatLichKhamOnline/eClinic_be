#!/usr/bin/env node
/*
 * Tìm ứng viên chân dung bác sĩ mẫu trên Unsplash (DOANTOTNGH-28 phase 4), bù phần Pixabay không đủ. Chỉ tìm và lọc,
 * không tải ảnh, không sửa nguon.json: người chạy xem bảng ảnh rồi chọn tay.
 *
 *   node scripts/anh-mau/tim-unsplash.js <file-ket-qua.json> [so-trang] [NU|NAM] ["truy van 1|truy van 2"]
 *
 * 2 tham số cuối để tìm thêm 1 giới tính với truy vấn khác (trang 1..so-trang của các truy vấn đó).
 *
 * Đọc UNSPLASH_ACCESS_KEY từ .env ở thư mục gốc (không bao giờ in ra). Khoá demo chỉ có 50 request / giờ: mặc định
 * 2 trang x 3 truy vấn x 2 giới tính = 12 request. Lọc:
 *  - bỏ ảnh Unsplash+ (plus.unsplash.com, không theo giấy phép Unsplash) và ảnh tài trợ;
 *  - alt_description nói đúng 1 giới tính (khớp cả từ: "woman" chứa "man"), không có từ nhóm / khẩu trang;
 *  - cạnh ngắn >= 1500 px; mỗi tác giả tối đa 2 ảnh (ảnh trùng người mẫu loại tay lúc xem bảng ảnh).
 * Ghi kèm links.download_location: nguyên tắc API Unsplash yêu cầu gọi nó khi dùng ảnh (tai-len.js gọi lúc tải lên).
 */
const fs = require('fs');
const path = require('path');

const GOC = path.resolve(__dirname, '..', '..');
const TOI_DA_MOI_TAC_GIA = 2;

const TRUY_VAN = {
    NU: ['female doctor portrait', 'woman doctor', 'female physician white coat'],
    NAM: ['male doctor portrait', 'man doctor', 'male physician white coat'],
};
const TU = {
    NU: /\b(woman|women|female|girl|lady)\b/i,
    NAM: /\b(man|men|male|boy|guy|gentleman)\b/i,
};
const TU_LOAI = /\b(women|men|people|group|team|colleagues|doctors|nurses|couple|family|child|children|kid|kids|baby|patient|patients|mask|masks|covid|coronavirus|two|three|crowd)\b/i;

function docKhoa() {
    for (const dong of fs.readFileSync(path.join(GOC, '.env'), 'utf8').split(/\r?\n/)) {
        const m = dong.match(/^\s*UNSPLASH_ACCESS_KEY\s*=\s*(.*?)\s*$/);
        if (m && m[1]) return m[1].replace(/^["']|["']$/g, '');
    }
    throw new Error('Thiếu UNSPLASH_ACCESS_KEY trong .env');
}

async function tim(khoa, query, page) {
    const url = 'https://api.unsplash.com/search/photos?' + new URLSearchParams({
        query, page: String(page), per_page: '30', content_filter: 'high',
    });
    const res = await fetch(url, {
        headers: { Authorization: `Client-ID ${khoa}`, 'Accept-Version': 'v1' }, signal: AbortSignal.timeout(30000),
    });
    if (!res.ok) throw new Error(`Unsplash HTTP ${res.status} (${query}, trang ${page})`);
    return { results: (await res.json()).results ?? [], conLai: res.headers.get('x-ratelimit-remaining') };
}

/** 'photo-1580281658626-ee379f3cce93' trong urls.raw -> nguon 'u:1580281658626-ee379f3cce93' như nguon.json đang dùng. */
function maAnh(p) {
    const m = p.urls?.raw?.match(/^https:\/\/images\.unsplash\.com\/photo-([A-Za-z0-9-]+)/);
    return m ? m[1] : null;
}

async function main() {
    const [fileKetQua, soTrangArg, chiGioiTinh, truyVanArg] = process.argv.slice(2);
    if (!fileKetQua) throw new Error('Cách dùng: node tim-unsplash.js <file-ket-qua.json> [so-trang]');
    const soTrang = Number(soTrangArg ?? 2);
    const khoa = docKhoa();
    const ds = [];
    const daCo = new Set();
    let conLai = null;
    for (const gioiTinh of chiGioiTinh ? [chiGioiTinh] : ['NU', 'NAM']) {
        const khac = gioiTinh === 'NU' ? 'NAM' : 'NU';
        const tacGia = new Map();
        for (let page = 1; page <= soTrang; page++) {
            for (const q of truyVanArg ? truyVanArg.split('|') : TRUY_VAN[gioiTinh]) {
                const r = await tim(khoa, q, page);
                conLai = r.conLai;
                for (const p of r.results) {
                    const ma = maAnh(p);
                    const moTa = `${p.alt_description ?? ''} ${p.description ?? ''}`;
                    if (!ma || daCo.has(p.id) || p.sponsorship || Math.min(p.width, p.height) < 1500
                            || !TU[gioiTinh].test(moTa) || TU[khac].test(moTa) || TU_LOAI.test(moTa)
                            || (tacGia.get(p.user.id) ?? 0) >= TOI_DA_MOI_TAC_GIA) {
                        continue;
                    }
                    daCo.add(p.id);
                    tacGia.set(p.user.id, (tacGia.get(p.user.id) ?? 0) + 1);
                    ds.push({ id: p.id, nguon: `u:${ma}`, gioiTinhTag: gioiTinh, tacGia: p.user.name,
                        idTacGia: p.user.id, trangNguon: p.links.html, taiVe: p.links.download_location,
                        xemTruoc: p.urls.small, rong: p.width, cao: p.height, moTa: p.alt_description });
                }
            }
        }
    }
    fs.writeFileSync(fileKetQua, JSON.stringify(ds, null, 2));
    const dem = ds.reduce((m, x) => ({ ...m, [x.gioiTinhTag]: (m[x.gioiTinhTag] ?? 0) + 1 }), {});
    console.log(`${ds.length} ứng viên ${JSON.stringify(dem)}; còn ${conLai} request trong giờ này`);
}

main().catch((e) => { console.error(e.message); process.exit(1); });
