#!/usr/bin/env node
/*
 * Tải ảnh mẫu (DOANTOTNGH-27) lên Cloudinary một lần, thu nhỏ ngay lúc nhận (incoming transformation) nên
 * Cloudinary chỉ giữ bản nhỏ. Ảnh nguồn chỉ nằm trong bộ nhớ, không ghi ra đĩa.
 *
 *   node scripts/anh-mau/tai-len.js              tải tất cả trong nguon.json
 *   node scripts/anh-mau/tai-len.js --chi a,b    chỉ tải lại các id này (ghi đè cùng public_id)
 *
 * Đọc CLOUDINARY_CLOUD_NAME / API_KEY / API_SECRET / FOLDER từ .env ở thư mục gốc. Ghi anh-mau.json cạnh file này.
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
    'chan-dung': 'c_thumb,g_face,z_0.7,w_256,h_256,q_auto:eco',
    'cong-viec': 'c_fill,g_auto,w_800,h_533,q_auto:eco',
    'chung-chi': 'c_limit,w_600,h_800,q_auto:good',
    'giai-thuong': 'c_limit,w_800,h_800,q_auto:good',
    'chuyen-khoa': 'c_fill,g_auto,w_640,h_400,q_auto:eco',
};

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

function urlNguon(nguon, nhom) {
    if (!nguon.startsWith('u:')) return nguon;
    const rong = nhom === 'chan-dung' ? 800 : 1400;
    return `https://images.unsplash.com/photo-${nguon.slice(2)}?w=${rong}&q=85&fm=jpg`;
}

const ngu = (ms) => new Promise((r) => setTimeout(r, ms));

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
    const noiDung = await taiNguon(urlNguon(anh.nguon, anh.nhom));
    const thamSo = {
        format: 'jpg',
        invalidate: 'true',
        overwrite: 'true',
        public_id: `${env.CLOUDINARY_FOLDER || 'eclinic'}/mau/${anh.id}`,
        timestamp: String(Math.floor(Date.now() / 1000)),
        transformation: BIEN_DOI[anh.nhom],
    };
    const form = new FormData();
    for (const [k, v] of Object.entries(thamSo)) form.append(k, v);
    form.append('api_key', env.CLOUDINARY_API_KEY);
    form.append('signature', ky(thamSo, env.CLOUDINARY_API_SECRET));
    form.append('file', new Blob([noiDung], { type: 'image/jpeg' }), 'anh.jpg');
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
                ...(anh.gioiTinh && { gioiTinh: anh.gioiTinh }), ...(anh.chuyenKhoa && { chuyenKhoa: anh.chuyenKhoa }) };
            console.log(`ok  ${anh.id}  ${r.width}x${r.height}  ${(r.bytes / 1024).toFixed(1)} KB`);
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
