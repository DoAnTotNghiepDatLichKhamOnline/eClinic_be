#!/usr/bin/env node
// Tạo access token (JWT HS256) để thử các API cần đăng nhập trên Swagger khi CHƯA có API đăng nhập (AUTH-02).
// XOÁ file này khi identity-service đã có API đăng nhập.
//
// Cách dùng (chạy ở thư mục gốc project):
//   node scripts/tao-token-dev.js                       -> token quản trị viên, idTaiKhoan = 1
//   node scripts/tao-token-dev.js BAC_SI 5              -> token bác sĩ có idTaiKhoan = 5
//   node scripts/tao-token-dev.js BENH_NHAN 12
// Dán token vào nút "Authorize" của Swagger (không cần gõ "Bearer ").
//
// Khoá ký lấy theo thứ tự: biến môi trường JWT_SECRET -> dòng JWT_SECRET= trong file .env -> khoá mặc định
// giống application-common.yml và docker-compose.yml. Script KHÔNG in khoá ra màn hình.
// Token hết hạn sau 8 giờ. Chỉ dùng ở máy local.

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const VAI_TRO_HOP_LE = ['QUAN_TRI_VIEN', 'BAC_SI', 'BENH_NHAN'];
const KHOA_MAC_DINH = 'eclinic-dev-secret-chi-dung-o-may-local-khong-dung-that-2026';
const THOI_HAN_GIAY = 8 * 60 * 60;

function huongDan(thongBao) {
  console.error(thongBao);
  console.error('Cách dùng: node scripts/tao-token-dev.js [QUAN_TRI_VIEN|BAC_SI|BENH_NHAN] [idTaiKhoan]');
  process.exit(1);
}

/** Chỉ đọc đúng dòng JWT_SECRET= trong .env, bỏ qua mọi biến khác. */
function docKhoaTuFileEnv() {
  try {
    const noiDung = fs.readFileSync(path.join(__dirname, '..', '.env'), 'utf8').replace(/^﻿/, '');
    const dong = noiDung.split(/\r?\n/).find((d) => /^\s*JWT_SECRET\s*=/.test(d));
    // Bỏ dấu nháy bao quanh giống docker compose: JWT_SECRET="abc" -> abc
    const giaTri = dong ? dong.slice(dong.indexOf('=') + 1).trim().replace(/^(["'])(.*)\1$/, '$2') : '';
    return giaTri || null;
  } catch {
    return null;
  }
}

const vaiTro = (process.argv[2] || 'QUAN_TRI_VIEN').toUpperCase();
const idTaiKhoan = process.argv[3] || '1';
if (!VAI_TRO_HOP_LE.includes(vaiTro)) {
  huongDan(`Vai trò không hợp lệ: ${vaiTro}`);
}
if (!/^[1-9][0-9]*$/.test(idTaiKhoan)) {
  huongDan(`idTaiKhoan phải là số nguyên dương: ${idTaiKhoan}`);
}

let khoa = process.env.JWT_SECRET;
let nguonKhoa = 'biến môi trường JWT_SECRET';
if (!khoa) {
  khoa = docKhoaTuFileEnv();
  nguonKhoa = 'file .env';
}
if (!khoa) {
  khoa = KHOA_MAC_DINH;
  nguonKhoa = 'khoá mặc định (giống application-common.yml)';
}

const base64Url = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');
const bayGio = Math.floor(Date.now() / 1000);
const header = base64Url({ alg: 'HS256', typ: 'JWT' });
const payload = base64Url({
  iss: 'eclinic',
  sub: idTaiKhoan,
  email: `dev-${vaiTro.toLowerCase().replace(/_/g, '-')}@eclinic.local`,
  vaiTro,
  iat: bayGio,
  exp: bayGio + THOI_HAN_GIAY,
});
const chuKy = crypto.createHmac('sha256', khoa).update(`${header}.${payload}`).digest('base64url');

console.error(`Token ${vaiTro} (idTaiKhoan=${idTaiKhoan}, hết hạn sau 8 giờ), khoá ký lấy từ ${nguonKhoa}.`);
console.error('Dán vào Swagger -> Authorize:');
console.log(`${header}.${payload}.${chuKy}`);
