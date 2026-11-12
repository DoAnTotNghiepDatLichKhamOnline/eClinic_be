#!/usr/bin/env node
/*
 * Sinh common/.../common/dulieumau/AnhMau.java từ anh-mau.json (DOANTOTNGH-27), để 64 URL ảnh mẫu không phải
 * chép tay. Chạy lại sau mỗi lần tai-len.js đổi anh-mau.json:
 *
 *   node scripts/anh-mau/sinh-java.js
 *
 * Nhóm ảnh lấy theo tiền tố id (bac-si/, nguoi-dung/, cong-viec/, chung-chi/, chuyen-khoa/), giới tính lấy từ
 * trường gioiTinh của manifest. Thiếu nhóm hoặc thiếu bs01..bs12 thì dừng, không ghi file.
 */
const fs = require('fs');
const path = require('path');

const THU_MUC = __dirname;
const GOC = path.resolve(THU_MUC, '..', '..');
const FILE_MANIFEST = path.join(THU_MUC, 'anh-mau.json');
const FILE_JAVA = path.join(GOC, 'common', 'src', 'main', 'java', 'iuh', 'fit', 'se', 'eclinic', 'common',
    'dulieumau', 'AnhMau.java');
const SO_BAC_SI_CHINH = 12;

const manifest = JSON.parse(fs.readFileSync(FILE_MANIFEST, 'utf8'));
// nhom ở đây là tiền tố id (bac-si, nguoi-dung...), khác trường nhom của manifest (kiểu biến đổi ảnh)
const muc = Object.entries(manifest).map(([id, v]) => ({ ...v, id, nhom: id.split('/')[0], ten: id.split('/')[1] }));

function loc(nhom, gioiTinh) {
    const ds = muc.filter((m) => m.nhom === nhom && (!gioiTinh || m.gioiTinh === gioiTinh))
        .sort((a, b) => a.ten.localeCompare(b.ten));
    if (!ds.length) throw new Error(`Không có ảnh nào cho ${nhom}${gioiTinh ? ' ' + gioiTinh : ''}`);
    return ds;
}

const bacSiChinh = [];
for (let so = 1; so <= SO_BAC_SI_CHINH; so++) {
    const id = `bac-si/bs${String(so).padStart(2, '0')}`;
    if (!manifest[id]) throw new Error(`Thiếu ${id} trong anh-mau.json`);
    bacSiChinh.push({ id, ...manifest[id] });
}
const chuyenKhoa = loc('chuyen-khoa');
for (const m of chuyenKhoa) if (!m.chuyenKhoa) throw new Error(`${m.id} thiếu trường chuyenKhoa`);
if (new Set(chuyenKhoa.map((m) => m.chuyenKhoa)).size !== chuyenKhoa.length) throw new Error('Trùng tên chuyên khoa');

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

    /** Chân dung 12 bác sĩ mẫu chính: phần tử i là bác sĩ số i + 1 (nữ là số chẵn). */
    private static final List<String> BAC_SI = ${danhSach(bacSiChinh)};

    /** Chân dung bác sĩ nam (gồm các bác sĩ chính nam) cho bác sĩ mẫu số lượng lớn. */
    private static final List<String> BAC_SI_NAM = ${danhSach(loc('bac-si', 'NAM'))};

    private static final List<String> BAC_SI_NU = ${danhSach(loc('bac-si', 'NU'))};

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

    /** Mọi URL ảnh mẫu. */
    public static final Set<String> TAT_CA = Set.copyOf(Stream.of(BAC_SI, BAC_SI_NAM, BAC_SI_NU, NGUOI_DUNG_NAM,
            NGUOI_DUNG_NU, CONG_VIEC, CHUNG_CHI, GIAI_THUONG, List.copyOf(CHUYEN_KHOA.values())).flatMap(List::stream).toList());

    /** Chân dung riêng của bác sĩ mẫu chính số {@code so} (1-${SO_BAC_SI_CHINH}); ngoài khoảng đó trả null. */
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
console.log(`Đã ghi ${path.relative(GOC, FILE_JAVA)}: ${bacSiChinh.length} bác sĩ chính, ${chuyenKhoa.length} chuyên khoa,`
    + ` ${muc.length} ảnh trong manifest`);
