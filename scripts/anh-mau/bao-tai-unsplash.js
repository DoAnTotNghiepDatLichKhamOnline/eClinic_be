#!/usr/bin/env node
/*
 * Báo tải lại ảnh Unsplash (links.download_location, trường taiVe trong nguon.json) cho các mục tai-len.js chưa báo được
 * vì khoá demo hết 50 request / giờ (DOANTOTNGH-28 phase 4). Không tải ảnh, không sửa file nào.
 *
 *   node scripts/anh-mau/bao-tai-unsplash.js --chi bac-si/bs53,bac-si/bs54
 *
 * Đọc UNSPLASH_ACCESS_KEY từ .env ở thư mục gốc (không bao giờ in ra). Dừng ở lỗi đầu tiên (thường là lại hết quota).
 */
const fs = require('fs');
const path = require('path');

const GOC = path.resolve(__dirname, '..', '..');

function docKhoa() {
    for (const dong of fs.readFileSync(path.join(GOC, '.env'), 'utf8').split(/\r?\n/)) {
        const m = dong.match(/^\s*UNSPLASH_ACCESS_KEY\s*=\s*(.*?)\s*$/);
        if (m && m[1]) return m[1].replace(/^["']|["']$/g, '');
    }
    throw new Error('Thiếu UNSPLASH_ACCESS_KEY trong .env');
}

async function main() {
    const iChi = process.argv.indexOf('--chi');
    if (iChi < 0) throw new Error('Cách dùng: node bao-tai-unsplash.js --chi <id,id,...>');
    const chi = new Set(process.argv[iChi + 1].split(','));
    const khoa = docKhoa();
    const { anh } = JSON.parse(fs.readFileSync(path.join(__dirname, 'nguon.json'), 'utf8'));
    let so = 0;
    for (const a of anh) {
        if (!chi.has(a.id) || !a.taiVe) continue;
        const res = await fetch(a.taiVe, { headers: { Authorization: `Client-ID ${khoa}`, 'Accept-Version': 'v1' } });
        if (!res.ok) throw new Error(`${a.id}: HTTP ${res.status} sau ${so} lần báo (còn ${res.headers.get('x-ratelimit-remaining')})`);
        so++;
    }
    console.log(`Đã báo tải ${so} ảnh`);
}

main().catch((e) => { console.error(e.message); process.exit(1); });
