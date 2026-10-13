package iuh.fit.se.eclinic.catalog.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
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
 * 3 mục danh sách (đào tạo, công tác, lĩnh vực khám chữa) lưu trong DB là văn bản mỗi dòng 1 ý, DTO là mảng chuỗi.
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
                bacSi.getHocVi(), bacSi.chucVu(ngonNgu), bacSi.getSoNamKinhNghiem(), bacSi.gioiThieuNgan(ngonNgu),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().ten(ngonNgu),
                diem == null ? null : DiemDanhGia.lamTron(diem.getDiemTrungBinh()),
                diem == null ? 0 : diem.getSoDanhGia());
    }

    /** @param diem như {@link #toResponse} */
    public BacSiChiTietResponse toChiTietResponse(BacSi bacSi, List<AnhBacSi> danhSachAnh, DiemTheoBacSi diem) {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        return new BacSiChiTietResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(),
                bacSi.getTaiKhoan().getAnhDaiDien(), bacSi.getHocVi(), bacSi.chucVu(ngonNgu),
                bacSi.getSoNamKinhNghiem(), bacSi.gioiThieuNgan(ngonNgu), bacSi.getChuyenKhoa().getId(),
                bacSi.getChuyenKhoa().ten(ngonNgu), bacSi.tieuSu(ngonNgu), tachDong(bacSi.quaTrinhDaoTao(ngonNgu)),
                tachDong(bacSi.quaTrinhCongTac(ngonNgu)), tachDong(bacSi.linhVucKhamChua(ngonNgu)),
                danhSachAnh.stream().map(anh -> toAnhResponse(anh, ngonNgu)).toList(),
                diem == null ? null : DiemDanhGia.lamTron(diem.getDiemTrungBinh()),
                diem == null ? 0 : diem.getSoDanhGia());
    }

    /** Các mục giới thiệu: chữ đã lưu của cả 2 ngôn ngữ. Tên chuyên khoa: theo ngôn ngữ của request. */
    public HoSoBacSiQuanTriResponse toQuanTriResponse(BacSi bacSi, List<AnhBacSi> danhSachAnh) {
        TaiKhoan taiKhoan = bacSi.getTaiKhoan();
        return new HoSoBacSiQuanTriResponse(bacSi.getId(), maBacSi(bacSi.getId()), taiKhoan.getHoTen(),
                taiKhoan.getEmail(), taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), bacSi.getSoGiayPhep(),
                bacSi.getTrangThai(), taiKhoan.getTrangThai(), taiKhoan.isPhaiDoiMatKhau(), bacSi.getHocVi(),
                bacSi.getChucVu(), bacSi.getChucVuEn(), bacSi.getSoNamKinhNghiem(), bacSi.getGioiThieuNgan(),
                bacSi.getGioiThieuNganEn(), bacSi.getChuyenKhoa().getId(),
                bacSi.getChuyenKhoa().ten(NgonNgu.hienTai()), bacSi.getTieuSu(), bacSi.getTieuSuEn(),
                tachDong(bacSi.getQuaTrinhDaoTao()), tachDong(bacSi.getQuaTrinhDaoTaoEn()),
                tachDong(bacSi.getQuaTrinhCongTac()), tachDong(bacSi.getQuaTrinhCongTacEn()),
                tachDong(bacSi.getLinhVucKhamChua()), tachDong(bacSi.getLinhVucKhamChuaEn()),
                toAnhQuanTriResponse(danhSachAnh));
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

    /** Ghi đè mọi trường của hồ sơ giới thiệu, cả 2 ngôn ngữ: chuỗi được bỏ khoảng trắng thừa, rỗng thành null. */
    public void capNhatHoSo(BacSi bacSi, CapNhatHoSoBacSiRequest request) {
        bacSi.setHocVi(chuanHoa(request.hocVi()));
        bacSi.setChucVu(chuanHoa(request.chucVu()));
        bacSi.setChucVuEn(chuanHoa(request.chucVuEn()));
        bacSi.setSoNamKinhNghiem(request.soNamKinhNghiem());
        bacSi.setGioiThieuNgan(chuanHoa(request.gioiThieuNgan()));
        bacSi.setGioiThieuNganEn(chuanHoa(request.gioiThieuNganEn()));
        bacSi.setTieuSu(chuanHoa(request.tieuSu()));
        bacSi.setTieuSuEn(chuanHoa(request.tieuSuEn()));
        bacSi.setQuaTrinhDaoTao(gopDong(request.quaTrinhDaoTao()));
        bacSi.setQuaTrinhDaoTaoEn(gopDong(request.quaTrinhDaoTaoEn()));
        bacSi.setQuaTrinhCongTac(gopDong(request.quaTrinhCongTac()));
        bacSi.setQuaTrinhCongTacEn(gopDong(request.quaTrinhCongTacEn()));
        bacSi.setLinhVucKhamChua(gopDong(request.linhVucKhamChua()));
        bacSi.setLinhVucKhamChuaEn(gopDong(request.linhVucKhamChuaEn()));
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

    /** Danh sách ý -> văn bản mỗi dòng 1 ý. Danh sách null / rỗng -> null. */
    private static String gopDong(List<String> cacY) {
        if (cacY == null || cacY.isEmpty()) {
            return null;
        }
        return String.join("\n", cacY.stream().map(String::trim).toList());
    }

}
