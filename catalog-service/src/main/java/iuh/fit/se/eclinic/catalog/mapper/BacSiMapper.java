package iuh.fit.se.eclinic.catalog.mapper;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoCuaToiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.repository.DanhGiaChiDocRepository.DiemTheoBacSi;
import iuh.fit.se.eclinic.common.entity.catalog.AnhBacSi;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.util.DiemDanhGia;

/**
 * Chuyển entity bác sĩ sang DTO. Họ tên, ảnh đại diện lấy từ tài khoản của bác sĩ.
 * <p>
 * Các mục danh sách (đào tạo, công tác, lĩnh vực khám chữa, giải thưởng, nghiên cứu) lưu trong DB là văn bản mỗi dòng
 * 1 ý, DTO là mảng chuỗi. Ngôn ngữ khám lưu "vi,en", DTO là mảng mã.
 */
@Component
public class BacSiMapper {

    /**
     * Chữ của hồ sơ theo ngôn ngữ của request; họ tên, học vị không dịch.
     *
     * @param diem điểm đánh giá của bác sĩ; null nếu chưa có đánh giá nào
     */
    public BacSiResponse toResponse(BacSi bacSi, DiemTheoBacSi diem) {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        return new BacSiResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(), bacSi.getTaiKhoan().getAnhDaiDien(),
                bacSi.getHocVi(), bacSi.chucVu(ngonNgu), bacSi.soNamKinhNghiem(), bacSi.gioiThieuNgan(ngonNgu),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().ten(ngonNgu),
                diem == null ? null : DiemDanhGia.lamTron(diem.getDiemTrungBinh()),
                diem == null ? 0 : diem.getSoDanhGia());
    }

    /** @param diem như {@link #toResponse} */
    public BacSiChiTietResponse toChiTietResponse(BacSi bacSi, List<AnhBacSi> danhSachAnh, DiemTheoBacSi diem) {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        return new BacSiChiTietResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(),
                bacSi.getTaiKhoan().getAnhDaiDien(), bacSi.getHocVi(), bacSi.chucVu(ngonNgu),
                bacSi.soNamKinhNghiem(), bacSi.gioiThieuNgan(ngonNgu), bacSi.getChuyenKhoa().getId(),
                bacSi.getChuyenKhoa().ten(ngonNgu), bacSi.tieuSu(ngonNgu), tachDong(bacSi.quaTrinhDaoTao(ngonNgu)),
                tachDong(bacSi.quaTrinhCongTac(ngonNgu)), tachDong(bacSi.linhVucKhamChua(ngonNgu)),
                tachDong(bacSi.giaiThuong(ngonNgu)), tachDong(bacSi.congTrinhNghienCuu(ngonNgu)),
                tachNgonNgu(bacSi.getNgonNguKham()), bacSi.getSoGiayPhep(), bacSi.getNgayCapGiayPhep(),
                bacSi.noiCapGiayPhep(ngonNgu), bacSi.phamViHanhNghe(ngonNgu),
                danhSachAnh.stream().map(anh -> toAnhResponse(anh, ngonNgu)).toList(),
                diem == null ? null : DiemDanhGia.lamTron(diem.getDiemTrungBinh()),
                diem == null ? 0 : diem.getSoDanhGia());
    }

    /** Các mục giới thiệu: chữ đã lưu của cả 2 ngôn ngữ. Tên chuyên khoa: theo ngôn ngữ của request. */
    public HoSoBacSiQuanTriResponse toQuanTriResponse(BacSi bacSi, List<AnhBacSi> danhSachAnh) {
        TaiKhoan taiKhoan = bacSi.getTaiKhoan();
        return new HoSoBacSiQuanTriResponse(bacSi.getId(), maBacSi(bacSi.getId()), taiKhoan.getHoTen(),
                taiKhoan.getEmail(), taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), bacSi.getSoGiayPhep(),
                bacSi.getNgayCapGiayPhep(), bacSi.getNoiCapGiayPhep(), bacSi.getNoiCapGiayPhepEn(),
                bacSi.getPhamViHanhNghe(), bacSi.getPhamViHanhNgheEn(),
                bacSi.getTrangThai(), taiKhoan.getTrangThai(), taiKhoan.isPhaiDoiMatKhau(), bacSi.getHocVi(),
                bacSi.getChucVu(), bacSi.getChucVuEn(), bacSi.getNamBatDauHanhNghe(), bacSi.soNamKinhNghiem(),
                tachNgonNgu(bacSi.getNgonNguKham()), bacSi.getGioiThieuNgan(),
                bacSi.getGioiThieuNganEn(), bacSi.getChuyenKhoa().getId(),
                bacSi.getChuyenKhoa().ten(NgonNgu.hienTai()), bacSi.getTieuSu(), bacSi.getTieuSuEn(),
                tachDong(bacSi.getQuaTrinhDaoTao()), tachDong(bacSi.getQuaTrinhDaoTaoEn()),
                tachDong(bacSi.getQuaTrinhCongTac()), tachDong(bacSi.getQuaTrinhCongTacEn()),
                tachDong(bacSi.getLinhVucKhamChua()), tachDong(bacSi.getLinhVucKhamChuaEn()),
                tachDong(bacSi.getGiaiThuong()), tachDong(bacSi.getGiaiThuongEn()),
                tachDong(bacSi.getCongTrinhNghienCuu()), tachDong(bacSi.getCongTrinhNghienCuuEn()),
                bacSi.getPhienBanHoSo(), toAnhQuanTriResponse(danhSachAnh));
    }

    /** 1 dòng của danh bạ bác sĩ cho quản trị viên. */
    public BacSiQuanTriResponse toDongQuanTri(BacSi bacSi, long soLuotDaKham) {
        TaiKhoan taiKhoan = bacSi.getTaiKhoan();
        return new BacSiQuanTriResponse(bacSi.getId(), maBacSi(bacSi.getId()), taiKhoan.getHoTen(), taiKhoan.getEmail(),
                taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), bacSi.getHocVi(), bacSi.getSoGiayPhep(),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().ten(NgonNgu.hienTai()), soLuotDaKham,
                bacSi.getTrangThai(), taiKhoan.getTrangThai(), taiKhoan.isPhaiDoiMatKhau());
    }

    /** Mã hiển thị của bác sĩ: "BS" + id đủ 4 chữ số (BS0007). Suy ra từ id, không lưu trong DB. */
    public static String maBacSi(Long id) {
        return String.format("BS%04d", id);
    }

    /** Bản công khai: chú thích theo ngôn ngữ đã cho. */
    public AnhBacSiResponse toAnhResponse(AnhBacSi anh, NgonNgu ngonNgu) {
        return new AnhBacSiResponse(anh.getId(), anh.getLoai(), anh.getUrl(), anh.chuThich(ngonNgu));
    }

    public AnhBacSiQuanTriResponse toAnhQuanTriResponse(AnhBacSi anh) {
        return new AnhBacSiQuanTriResponse(anh.getId(), anh.getLoai(), anh.getUrl(), anh.getChuThich(),
                anh.getChuThichEn());
    }

    public List<AnhBacSiQuanTriResponse> toAnhQuanTriResponse(List<AnhBacSi> danhSachAnh) {
        return danhSachAnh.stream().map(this::toAnhQuanTriResponse).toList();
    }

    /**
     * Quản trị viên: ghi đè mọi trường của hồ sơ giới thiệu, cả 2 ngôn ngữ (chuỗi được bỏ khoảng trắng thừa, rỗng thành
     * null). Không đổi phiên bản hồ sơ: service tăng sau khi kiểm tra.
     */
    public void capNhatHoSo(BacSi bacSi, CapNhatHoSoBacSiRequest request) {
        ganPhanBacSiTuSua(bacSi, request.gioiThieuNgan(), request.gioiThieuNganEn(), request.tieuSu(),
                request.tieuSuEn(), request.ngonNguKham(), request.giaiThuong(), request.giaiThuongEn(),
                request.congTrinhNghienCuu(), request.congTrinhNghienCuuEn());
        bacSi.setHocVi(chuanHoa(request.hocVi()));
        bacSi.setChucVu(chuanHoa(request.chucVu()));
        bacSi.setChucVuEn(chuanHoa(request.chucVuEn()));
        bacSi.setNamBatDauHanhNghe(request.namBatDauHanhNghe());
        bacSi.setNgayCapGiayPhep(request.ngayCapGiayPhep());
        bacSi.setNoiCapGiayPhep(chuanHoa(request.noiCapGiayPhep()));
        bacSi.setNoiCapGiayPhepEn(chuanHoa(request.noiCapGiayPhepEn()));
        bacSi.setPhamViHanhNghe(chuanHoa(request.phamViHanhNghe()));
        bacSi.setPhamViHanhNgheEn(chuanHoa(request.phamViHanhNgheEn()));
        bacSi.setQuaTrinhDaoTao(gopDong(request.quaTrinhDaoTao()));
        bacSi.setQuaTrinhDaoTaoEn(gopDong(request.quaTrinhDaoTaoEn()));
        bacSi.setQuaTrinhCongTac(gopDong(request.quaTrinhCongTac()));
        bacSi.setQuaTrinhCongTacEn(gopDong(request.quaTrinhCongTacEn()));
        bacSi.setLinhVucKhamChua(gopDong(request.linhVucKhamChua()));
        bacSi.setLinhVucKhamChuaEn(gopDong(request.linhVucKhamChuaEn()));
    }

    /** Bác sĩ tự sửa: chỉ ghi đè phần của bác sĩ (B7), mục khác giữ nguyên. Không đổi phiên bản hồ sơ. */
    public void capNhatHoSoCuaToi(BacSi bacSi, CapNhatHoSoCuaToiRequest request) {
        ganPhanBacSiTuSua(bacSi, request.gioiThieuNgan(), request.gioiThieuNganEn(), request.tieuSu(),
                request.tieuSuEn(), request.ngonNguKham(), request.giaiThuong(), request.giaiThuongEn(),
                request.congTrinhNghienCuu(), request.congTrinhNghienCuuEn());
    }

    /** Các mục bác sĩ tự sửa được; dùng chung cho 2 bản sửa để không lệch nhau. */
    private void ganPhanBacSiTuSua(BacSi bacSi, String gioiThieuNgan, String gioiThieuNganEn, String tieuSu,
            String tieuSuEn, List<String> ngonNguKham, List<String> giaiThuong, List<String> giaiThuongEn,
            List<String> congTrinhNghienCuu, List<String> congTrinhNghienCuuEn) {
        bacSi.setGioiThieuNgan(chuanHoa(gioiThieuNgan));
        bacSi.setGioiThieuNganEn(chuanHoa(gioiThieuNganEn));
        bacSi.setTieuSu(chuanHoa(tieuSu));
        bacSi.setTieuSuEn(chuanHoa(tieuSuEn));
        bacSi.setNgonNguKham(gopNgonNgu(ngonNguKham));
        bacSi.setGiaiThuong(gopDong(giaiThuong));
        bacSi.setGiaiThuongEn(gopDong(giaiThuongEn));
        bacSi.setCongTrinhNghienCuu(gopDong(congTrinhNghienCuu));
        bacSi.setCongTrinhNghienCuuEn(gopDong(congTrinhNghienCuuEn));
    }

    /** Bỏ khoảng trắng thừa, chuỗi rỗng thành null. */
    public String chuanHoa(String giaTri) {
        return giaTri == null || giaTri.isBlank() ? null : giaTri.trim();
    }

    /** Văn bản mỗi dòng 1 ý -> danh sách ý, bỏ dòng trống. null -> danh sách rỗng. */
    private static List<String> tachDong(String vanBan) {
        if (vanBan == null) {
            return List.of();
        }
        return vanBan.lines().map(String::trim).filter(dong -> !dong.isEmpty()).toList();
    }

    /** Mã ngôn ngữ đã lưu ("vi,en") -> danh sách. null -> danh sách rỗng. */
    private static List<String> tachNgonNgu(String vanBan) {
        if (vanBan == null) {
            return List.of();
        }
        return Arrays.stream(vanBan.split(",")).filter(ma -> !ma.isBlank()).toList();
    }

    /** Danh sách mã ngôn ngữ -> "vi,en": viết thường, bỏ mã trùng, giữ thứ tự. Danh sách null / rỗng -> null. */
    private static String gopNgonNgu(List<String> cacMa) {
        if (cacMa == null || cacMa.isEmpty()) {
            return null;
        }
        return String.join(",", new LinkedHashSet<>(cacMa.stream().map(ma -> ma.trim().toLowerCase(Locale.ROOT)).toList()));
    }

    /** Danh sách ý -> văn bản mỗi dòng 1 ý. Danh sách null / rỗng -> null. */
    private static String gopDong(List<String> cacY) {
        if (cacY == null || cacY.isEmpty()) {
            return null;
        }
        return String.join("\n", cacY.stream().map(String::trim).toList());
    }

}
