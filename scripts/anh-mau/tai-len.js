#!/usr/bin/env node
/*
 * Tải ảnh mẫu (DOANTOTNGH-27, DOANTOTNGH-28) lên Cloudinary một lần, thu nhỏ ngay lúc nhận (incoming transformation)
 * nên Cloudinary chỉ giữ bản đã thu nhỏ. Ảnh nguồn chỉ nằm trong bộ nhớ, không ghi ra đĩa.
 *
 * Từ DOANTOTNGH-28 ảnh nằm ở <folder>/mau-hd/<id> (cỡ lớn hơn, q_auto:good). Bản cũ <folder>/mau/<id> vẫn giữ: DB cũ
 * được đổi URL lúc khởi động theo anh-mau-v1.json (xem sinh-java.js, AnhMau.URL_CU).
 *
 *   node scripts/anh-mau/tai-len.js              tải tất cả trong nguon.json
 *   node scripts/anh-mau/tai-len.js --chi a,b    chỉ tải lại các id này (ghi đè cùng public_id)
 *
 * Đọc CLOUDINARY_CLOUD_NAME / API_KEY / API_SECRET / FOLDER từ .env ở thư mục gốc. Ghi anh-mau.json cạnh file này.
 * Nguồn 'pb:<id>' (Pixabay) cần PIXABAY_API_KEY; mục có taiVe (Unsplash) được báo tải nếu có UNSPLASH_ACCESS_KEY.
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const THU_MUC = __dirname;
const GOC = path.resolve(THU_MUC, '..', '..');
const FILE_NGUON = path.join(THU_MUC, 'nguon.json');
const FILE_KET_QUA = path.join(THU_MUC, 'anh-mau.json');
const UA = 'eclinic-sample-image-uploader/1.0 (school project)';

/** Biến đổi lúc nhận theo nhóm: cạnh nhỏ nhất mà vẫn rõ. */
const BIEN_DOI = {
    // c_thumb + g_face phóng vào khuôn mặt (ảnh toàn thân vẫn ra mặt đủ lớn); z < 1 chừa lại vai
    'chan-dung': 'c_thumb,g_face,z_0.7,w_512,h_512,q_auto:good',
    'cong-viec': 'c_fill,g_auto,w_1200,h_800,q_auto:good',
    'chung-chi': 'c_limit,w_1200,h_1600,q_auto:good',
    'giai-thuong': 'c_limit,w_1200,h_1200,q_auto:good',
    // Biểu tượng chuyên khoa: SVG vẽ ra PNG nền trong suốt
    'bieu-tuong': 'c_pad,w_512,h_512,b_transparent',
};

/** Định dạng lưu theo nhóm (mặc định jpg). */
const DINH_DANG = { 'bieu-tuong': 'png' };

/** Màu biểu tượng chuyên khoa (đổi màu: sửa rồi chạy lại --chi chuyen-khoa/...). */
const MAU_BIEU_TUONG = '#0E7490';

function docEnv() {
    const env = {};
    for (const dong of fs.readFileSync(path.join(GOC, '.env'), 'utf8').split(/\r?\n/)) {
        const m = dong.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/);
        if (m) env[m[1]] = m[2].replace(/^["']|["']$/g, '');
    }
    for (const k of ['CLOUDINARY_CLOUD_NAME', 'CLOUDINARY_API_KEY', 'CLOUDINARY_API_SECRET']) {
        if (!env[k]) throw new Error(`Thiếu ${k} trong .env`);
    }
    return env;
}

/**
 * Ảnh Pixabay ('pb:<id>'): URL ảnh của Pixabay chỉ dùng tạm, nên mỗi lần tải hỏi lại API theo id
 * (largeImageURL, cạnh dài 1280 px). Cần PIXABAY_API_KEY trong .env.
 */
async function urlPixabay(env, id) {
    if (!env.PIXABAY_API_KEY) throw new Error('Thiếu PIXABAY_API_KEY trong .env');
    const res = await fetch('https://pixabay.com/api/?' + new URLSearchParams({ key: env.PIXABAY_API_KEY, id }));
    if (!res.ok) throw new Error(`Pixabay HTTP ${res.status}`);
    const anh = (await res.json()).hits?.[0];
    if (!anh?.largeImageURL) throw new Error(`Pixabay không còn ảnh ${id}`);
    return anh.largeImageURL;
}

/**
 * Nguyên tắc API Unsplash: ảnh lấy qua API phải được "báo tải" (links.download_location) khi dùng. Lỗi (vd. hết 50
 * request / giờ của khoá demo) chỉ cảnh báo, không chặn việc tải lên; chạy lại --chi sau 1 giờ để báo lại.
 */
async function baoTaiUnsplash(env, taiVe) {
    if (!env.UNSPLASH_ACCESS_KEY) return 'bỏ qua báo tải (thiếu UNSPLASH_ACCESS_KEY)';
    const res = await fetch(taiVe, { headers: { Authorization: `Client-ID ${env.UNSPLASH_ACCESS_KEY}`,
        'Accept-Version': 'v1' } });
    return res.ok ? null : `báo tải Unsplash HTTP ${res.status}`;
}

function urlNguon(nguon) {
    if (nguon.startsWith('i:')) {
        return `https://api.iconify.design/healthicons/${nguon.slice(2)}.svg?color=${encodeURIComponent(MAU_BIEU_TUONG)}`
            + '&width=512&height=512';
    }
    if (!nguon.startsWith('u:')) return nguon;
    // Lấy nguồn lớn: chân dung còn bị cắt sát mặt nên nguồn nhỏ làm ảnh mờ
    return `https://images.unsplash.com/photo-${nguon.slice(2)}?w=2000&q=90&fm=jpg`;
}

const ngu = (ms) => new Promise((r) => setTimeout(r, ms));

/**
 * Biểu tượng chưa có khung (ib:<tên>): đặt hình màu trắng vào khung vuông bo góc cùng màu, đúng kích thước khung của
 * nhóm Specialties trong Health Icons (viewBox 48, khung 6..42, bo góc 3), để 30 biểu tượng trông như một bộ.
 */
async function bieuTuongCoKhung(ten) {
    const json = JSON.parse((await taiNguon(`https://api.iconify.design/healthicons.json?icons=${ten}`)).toString('utf8'));
    const icon = json.icons?.[ten];
    if (!icon) throw new Error(`không có biểu tượng ${ten}`);
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 48 48">`
        + `<rect x="6" y="6" width="36" height="36" rx="3" fill="${MAU_BIEU_TUONG}"/>`
        + `<g transform="translate(10.8 10.8) scale(0.55)" style="color:#fff">${icon.body}</g></svg>`;
    return Buffer.from(svg, 'utf8');
}

async function taiNguon(url) {
    for (const cho of [3000, 10000, 30000, 60000]) {
        if (url.includes('wikimedia.org')) await ngu(1500);
        const res = await fetch(url, { headers: { 'User-Agent': UA } });
        if (res.ok) return Buffer.from(await res.arrayBuffer());
        if (res.status !== 429 && res.status < 500) throw new Error(`nguồn HTTP ${res.status}`);
        await ngu(cho);
    }
    throw new Error('nguồn vẫn lỗi sau khi thử lại');
}

/** Chữ ký Upload API: tham số xếp theo tên, nối a=1&b=2, thêm secret, SHA-1 hex (như LuuTruAnhCloudinary.ky). */
function ky(thamSo, secret) {
    const chuoi = Object.keys(thamSo).sort().map((k) => `${k}=${thamSo[k]}`).join('&');
    return crypto.createHash('sha1').update(chuoi + secret).digest('hex');
}

async function taiLen(env, anh) {
    const laBieuTuong = anh.nguon.startsWith('i:') || anh.nguon.startsWith('ib:');
    const noiDung = anh.nguon.startsWith('ib:') ? await bieuTuongCoKhung(anh.nguon.slice(3))
        : await taiNguon(anh.nguon.startsWith('pb:') ? await urlPixabay(env, anh.nguon.slice(3)) : urlNguon(anh.nguon));
    const thamSo = {
        format: DINH_DANG[anh.nhom] ?? 'jpg',
        invalidate: 'true',
        overwrite: 'true',
        public_id: `${env.CLOUDINARY_FOLDER || 'eclinic'}/mau-hd/${anh.id}`,
        timestamp: String(Math.floor(Date.now() / 1000)),
        transformation: BIEN_DOI[anh.nhom],
    };
    const form = new FormData();
    for (const [k, v] of Object.entries(thamSo)) form.append(k, v);
    form.append('api_key', env.CLOUDINARY_API_KEY);
    form.append('signature', ky(thamSo, env.CLOUDINARY_API_SECRET));
    form.append('file', new Blob([noiDung], { type: laBieuTuong ? 'image/svg+xml' : 'image/jpeg' }),
        laBieuTuong ? 'anh.svg' : 'anh.jpg');
    const res = await fetch(`https://api.cloudinary.com/v1_1/${env.CLOUDINARY_CLOUD_NAME}/image/upload`,
        { method: 'POST', body: form });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(`Cloudinary HTTP ${res.status}: ${json.error?.message ?? ''}`.slice(0, 300));
    return { url: json.secure_url, bytes: json.bytes, width: json.width, height: json.height };
}

async function main() {
    const env = docEnv();
    const { anh: danhSach } = JSON.parse(fs.readFileSync(FILE_NGUON, 'utf8'));
    const iChi = process.argv.indexOf('--chi');
    const chi = iChi > 0 ? new Set(process.argv[iChi + 1].split(',')) : null;
    const ketQua = fs.existsSync(FILE_KET_QUA) ? JSON.parse(fs.readFileSync(FILE_KET_QUA, 'utf8')) : {};
    const loi = [];

    for (const anh of danhSach) {
        if (chi && !chi.has(anh.id)) continue;
        if (!BIEN_DOI[anh.nhom]) { loi.push(`${anh.id}: nhóm lạ ${anh.nhom}`); continue; }
        try {
            const r = await taiLen(env, anh);
            ketQua[anh.id] = { ...r, nhom: anh.nhom,
                ...(anh.gioiTinh && { gioiTinh: anh.gioiTinh }), ...(anh.chuyenKhoa && { chuyenKhoa: anh.chuyenKhoa }),
                ...(anh.tacGia && { tacGia: anh.tacGia, trangNguon: anh.trangNguon }) };
            const canhBao = anh.taiVe ? await baoTaiUnsplash(env, anh.taiVe) : null;
            console.log(`ok  ${anh.id}  ${r.width}x${r.height}  ${(r.bytes / 1024).toFixed(1)} KB`
                + (canhBao ? `  (cảnh báo: ${canhBao})` : ''));
        } catch (e) {
            loi.push(`${anh.id}: ${e.message}`);
            console.log(`LỖI ${anh.id}: ${e.message}`);
        }
    }
    // Giữ đúng thứ tự của nguon.json, bỏ id không còn trong nguồn
    const xep = {};
    for (const anh of danhSach) if (ketQua[anh.id]) xep[anh.id] = ketQua[anh.id];
    fs.writeFileSync(FILE_KET_QUA, JSON.stringify(xep, null, 2) + '\n');

    const tong = Object.values(xep).reduce((s, r) => s + r.bytes, 0);
    console.log(`\n${Object.keys(xep).length}/${danhSach.length} ảnh, tổng ${(tong / 1024).toFixed(0)} KB`);
    if (loi.length) {
        console.error(`\n${loi.length} lỗi:\n  ` + loi.join('\n  '));
        process.exit(1);
    }
}

main().catch((e) => { console.error(e.message); process.exit(1); });
