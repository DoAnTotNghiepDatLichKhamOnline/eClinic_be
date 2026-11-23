#!/usr/bin/env node
/*
 * Sinh common/.../common/dulieumau/AnhMau.java từ anh-mau.json (DOANTOTNGH-27), để 64 URL ảnh mẫu không phải
 * chép tay. Chạy lại sau mỗi lần tai-len.js đổi anh-mau.json:
 *
 *   node scripts/anh-mau/sinh-java.js
 *
 * Nhóm ảnh lấy theo tiền tố id (bac-si/, nguoi-dung/, cong-viec/, chung-chi/, chuyen-khoa/), giới tính lấy từ
 * trường gioiTinh của manifest. Thiếu nhóm, thiếu 1 trong bs01..bs100, hoặc giới tính của bsNN không khớp số (lẻ NAM, chẵn NU,
 * như DuLieuMauLon.gioiTinh) thì dừng, không ghi file.
 *
 * anh-mau-v1.json là manifest cũ (DOANTOTNGH-27, thư mục mau/) giữ nguyên để sinh URL_CU: URL cũ -> URL mới cùng id, dùng
 * khi khởi động để đổi URL ảnh mẫu đã lưu trong DB cũ (DOANTOTNGH-28). Id cũ nào không còn trong manifest mới thì dừng.
 */
const fs = require('fs');
const path = require('path');

const THU_MUC = __dirname;
const GOC = path.resolve(THU_MUC, '..', '..');
const FILE_MANIFEST = path.join(THU_MUC, 'anh-mau.json');
const FILE_MANIFEST_CU = path.join(THU_MUC, 'anh-mau-v1.json');
const FILE_JAVA = path.join(GOC, 'common', 'src', 'main', 'java', 'iuh', 'fit', 'se', 'eclinic', 'common',
    'dulieumau', 'AnhMau.java');
/** Mỗi bác sĩ mẫu (12 bác sĩ chính + bác sĩ mẫu lớn 13..100) có chân dung riêng bac-si/bsNN (DOANTOTNGH-28). */
const SO_BAC_SI = 100;

const manifest = JSON.parse(fs.readFileSync(FILE_MANIFEST, 'utf8'));
// nhom ở đây là tiền tố id (bac-si, nguoi-dung...), khác trường nhom của manifest (kiểu biến đổi ảnh)
const muc = Object.entries(manifest).map(([id, v]) => ({ ...v, id, nhom: id.split('/')[0], ten: id.split('/')[1] }));

function loc(nhom, gioiTinh) {
    const ds = muc.filter((m) => m.nhom === nhom && (!gioiTinh || m.gioiTinh === gioiTinh))
        .sort((a, b) => a.ten.localeCompare(b.ten));
    if (!ds.length) throw new Error(`Không có ảnh nào cho ${nhom}${gioiTinh ? ' ' + gioiTinh : ''}`);
    return ds;
}

const bacSiRieng = [];
for (let so = 1; so <= SO_BAC_SI; so++) {
    const id = `bac-si/bs${String(so).padStart(2, '0')}`;
    if (!manifest[id]) throw new Error(`Thiếu ${id} trong anh-mau.json`);
    const gioiTinh = so % 2 === 0 ? 'NU' : 'NAM';
    if (manifest[id].gioiTinh !== gioiTinh) throw new Error(`${id} là ${manifest[id].gioiTinh}, bác sĩ số ${so} là ${gioiTinh}`);
    bacSiRieng.push({ id, ...manifest[id] });
}
/** Bộ ảnh dùng chung cũ (bs01..bs12, nam01/02): không lặp lại 88 chân dung riêng trong file sinh ra. */
const chung = (gioiTinh) => loc('bac-si', gioiTinh).filter((m) => !/^bsd+$/.test(m.ten) || Number(m.ten.slice(2)) <= 12);
const chuyenKhoa = loc('chuyen-khoa');
for (const m of chuyenKhoa) if (!m.chuyenKhoa) throw new Error(`${m.id} thiếu trường chuyenKhoa`);
if (new Set(chuyenKhoa.map((m) => m.chuyenKhoa)).size !== chuyenKhoa.length) throw new Error('Trùng tên chuyên khoa');

const manifestCu = JSON.parse(fs.readFileSync(FILE_MANIFEST_CU, 'utf8'));
const urlCu = Object.entries(manifestCu).map(([id, v]) => {
    if (!manifest[id]) throw new Error(`${id} có trong anh-mau-v1.json nhưng không có trong anh-mau.json`);
    if (manifest[id].url === v.url) throw new Error(`${id} chưa được tải lại (URL mới trùng URL cũ)`);
    return [v.url, manifest[id].url];
});

const chuoi = (s) => '"' + s.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"';
const danhSach = (ds) => 'List.of(\n' + ds.map((m) => `            ${chuoi(m.url)}`).join(',\n') + ')';

const nguon = `package iuh.fit.se.eclinic.common.dulieumau;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import iuh.fit.se.eclinic.common.enums.GioiTinh;

/**
 * URL ảnh mẫu thật trên Cloudinary ({@code eclinic/mau/...}) cho dữ liệu mẫu, DOANTOTNGH-27.
 * <p>
 * FILE TỰ SINH, không sửa tay: sửa scripts/anh-mau/anh-mau.json (qua tai-len.js) rồi chạy
 * {@code node scripts/anh-mau/sinh-java.js}.
 * <p>
 * Ảnh dùng chung cho nhiều bản ghi và không thuộc bản ghi nào: ảnh giới thiệu mẫu để {@code maLuuTru} null, còn
 * xoá ảnh đại diện chỉ xoá khoá riêng của tài khoản, nên không thao tác nào xoá được ảnh trong thư mục này.
 */
public final class AnhMau {

    private AnhMau() {
    }

    /** Chân dung riêng của ${SO_BAC_SI} bác sĩ mẫu: phần tử i là bác sĩ số i + 1 (nữ là số chẵn). */
    private static final List<String> BAC_SI = ${danhSach(bacSiRieng)};

    /** Chân dung dùng chung theo giới tính (bác sĩ chính + nam01/02), chỉ cho bác sĩ mẫu số lớn hơn ${SO_BAC_SI}. */
    private static final List<String> BAC_SI_NAM = ${danhSach(chung('NAM'))};

    private static final List<String> BAC_SI_NU = ${danhSach(chung('NU'))};

    /** Ảnh đại diện bệnh nhân / quản trị viên (thường phục, không áo blouse). */
    private static final List<String> NGUOI_DUNG_NAM = ${danhSach(loc('nguoi-dung', 'NAM'))};

    private static final List<String> NGUOI_DUNG_NU = ${danhSach(loc('nguoi-dung', 'NU'))};

    /** Ảnh làm việc (khám, tư vấn, phòng khám). */
    private static final List<String> CONG_VIEC = ${danhSach(loc('cong-viec'))};

    /** Ảnh chứng chỉ / bằng cấp. */
    private static final List<String> CHUNG_CHI = ${danhSach(loc('chung-chi'))};

    /** Ảnh giải thưởng (cúp, huy chương). */
    private static final List<String> GIAI_THUONG = ${danhSach(loc('giai-thuong'))};

    /** Tên chuyên khoa mẫu (đúng như DuLieuMau / DuLieuMauLon) -> ảnh minh hoạ. */
    public static final Map<String, String> CHUYEN_KHOA = Map.ofEntries(
${chuyenKhoa.map((m) => `            Map.entry(${chuoi(m.chuyenKhoa)}, ${chuoi(m.url)})`).join(',\n')});

    /**
     * URL ảnh mẫu cũ (thư mục {@code eclinic/mau/}, DOANTOTNGH-27) -> URL mới cùng ảnh ({@code eclinic/mau-hd/}, cỡ lớn
     * hơn; chuyên khoa thành biểu tượng), để đổi URL đã lưu trong DB cũ. URL cũ vẫn mở được trên Cloudinary.
     */
    public static final Map<String, String> URL_CU = Map.ofEntries(
${urlCu.map(([cu, moi]) => `            Map.entry(${chuoi(cu)},\n                    ${chuoi(moi)})`).join(',\n')});

    /** Mọi URL ảnh mẫu. */
    public static final Set<String> TAT_CA = Set.copyOf(Stream.of(BAC_SI, BAC_SI_NAM, BAC_SI_NU, NGUOI_DUNG_NAM,
            NGUOI_DUNG_NU, CONG_VIEC, CHUNG_CHI, GIAI_THUONG, List.copyOf(CHUYEN_KHOA.values())).flatMap(List::stream).toList());

    /** Chân dung riêng của bác sĩ mẫu số {@code so} (1-${SO_BAC_SI}); ngoài khoảng đó trả null. */
    public static String bacSi(int so) {
        return so >= 1 && so <= BAC_SI.size() ? BAC_SI.get(so - 1) : null;
    }

    /** Chân dung bác sĩ cùng giới tính, xoay vòng theo {@code so} (KHAC dùng ảnh nam). */
    public static String bacSiTheoGioiTinh(GioiTinh gioiTinh, int so) {
        return chon(gioiTinh == GioiTinh.NU ? BAC_SI_NU : BAC_SI_NAM, so);
    }

    /** Ảnh đại diện người dùng cùng giới tính, xoay vòng theo {@code so} (KHAC dùng ảnh nam). */
    public static String nguoiDung(GioiTinh gioiTinh, int so) {
        return chon(gioiTinh == GioiTinh.NU ? NGUOI_DUNG_NU : NGUOI_DUNG_NAM, so);
    }

    public static String congViec(int i) {
        return chon(CONG_VIEC, i);
    }

    public static String chungChi(int i) {
        return chon(CHUNG_CHI, i);
    }

    public static String giaiThuong(int i) {
        return chon(GIAI_THUONG, i);
    }

    private static String chon(List<String> danhSach, int i) {
        return danhSach.get(Math.floorMod(i, danhSach.size()));
    }
}
`;

fs.mkdirSync(path.dirname(FILE_JAVA), { recursive: true });
fs.writeFileSync(FILE_JAVA, nguon.replace(/\r?\n/g, '\r\n'), 'utf8');
console.log(`Đã ghi ${path.relative(GOC, FILE_JAVA)}: ${bacSiRieng.length} bác sĩ, ${chuyenKhoa.length} chuyên khoa,`
    + ` ${muc.length} ảnh trong manifest, ${urlCu.length} URL cũ`);
