package iuh.fit.se.eclinic.catalog.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.repository.DanhGiaChiDocRepository.DiemTheoBacSi;
import iuh.fit.se.eclinic.common.entity.catalog.AnhBacSi;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.util.DiemDanhGia;

/**
 * Chuyển entity bác sĩ sang DTO. Họ tên, ảnh đại diện lấy từ tài khoản của bác sĩ.
 * <p>
 * 3 mục danh sách (đào tạo, công tác, lĩnh vực khám chữa) lưu trong DB là văn bản mỗi dòng 1 ý, DTO là mảng chuỗi.
 */
@Component
public class BacSiMapper {

    /** @param diem điểm đánh giá của bác sĩ; null nếu chưa có đánh giá nào */
    public BacSiResponse toResponse(BacSi bacSi, DiemTheoBacSi diem) {
        return new BacSiResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(), bacSi.getTaiKhoan().getAnhDaiDien(),
                bacSi.getHocVi(), bacSi.getChucVu(), bacSi.getSoNamKinhNghiem(), bacSi.getGioiThieuNgan(),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa(),
                diem == null ? null : DiemDanhGia.lamTron(diem.getDiemTrungBinh()),
                diem == null ? 0 : diem.getSoDanhGia());
    }

    /** @param diem như {@link #toResponse} */
    public BacSiChiTietResponse toChiTietResponse(BacSi bacSi, List<AnhBacSi> danhSachAnh, DiemTheoBacSi diem) {
        return new BacSiChiTietResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(),
                bacSi.getTaiKhoan().getAnhDaiDien(), bacSi.getHocVi(), bacSi.getChucVu(), bacSi.getSoNamKinhNghiem(),
                bacSi.getGioiThieuNgan(), bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa(),
                bacSi.getTieuSu(), tachDong(bacSi.getQuaTrinhDaoTao()), tachDong(bacSi.getQuaTrinhCongTac()),
                tachDong(bacSi.getLinhVucKhamChua()), toAnhResponse(danhSachAnh),
                diem == null ? null : DiemDanhGia.lamTron(diem.getDiemTrungBinh()),
                diem == null ? 0 : diem.getSoDanhGia());
    }

    public HoSoBacSiQuanTriResponse toQuanTriResponse(BacSi bacSi, List<AnhBacSi> danhSachAnh) {
        TaiKhoan taiKhoan = bacSi.getTaiKhoan();
        return new HoSoBacSiQuanTriResponse(bacSi.getId(), maBacSi(bacSi.getId()), taiKhoan.getHoTen(),
                taiKhoan.getEmail(), taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), bacSi.getSoGiayPhep(),
                bacSi.getTrangThai(), taiKhoan.getTrangThai(), taiKhoan.isPhaiDoiMatKhau(), bacSi.getHocVi(),
                bacSi.getChucVu(), bacSi.getSoNamKinhNghiem(), bacSi.getGioiThieuNgan(),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa(), bacSi.getTieuSu(),
                tachDong(bacSi.getQuaTrinhDaoTao()), tachDong(bacSi.getQuaTrinhCongTac()),
                tachDong(bacSi.getLinhVucKhamChua()), toAnhResponse(danhSachAnh));
    }

    /** 1 dòng của danh bạ bác sĩ cho quản trị viên. */
    public BacSiQuanTriResponse toDongQuanTri(BacSi bacSi, long soLuotDaKham) {
        TaiKhoan taiKhoan = bacSi.getTaiKhoan();
        return new BacSiQuanTriResponse(bacSi.getId(), maBacSi(bacSi.getId()), taiKhoan.getHoTen(), taiKhoan.getEmail(),
                taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), bacSi.getHocVi(), bacSi.getSoGiayPhep(),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa(), soLuotDaKham,
                bacSi.getTrangThai(), taiKhoan.getTrangThai(), taiKhoan.isPhaiDoiMatKhau());
    }

    /** Mã hiển thị của bác sĩ: "BS" + id đủ 4 chữ số (BS0007). Suy ra từ id, không lưu trong DB. */
    public static String maBacSi(Long id) {
        return String.format("BS%04d", id);
    }

    public AnhBacSiResponse toAnhResponse(AnhBacSi anh) {
        return new AnhBacSiResponse(anh.getId(), anh.getLoai(), anh.getUrl(), anh.getChuThich());
    }

    public List<AnhBacSiResponse> toAnhResponse(List<AnhBacSi> danhSachAnh) {
        return danhSachAnh.stream().map(this::toAnhResponse).toList();
    }

    /** Ghi đè mọi trường của hồ sơ giới thiệu: chuỗi được bỏ khoảng trắng thừa, rỗng thành null. */
    public void capNhatHoSo(BacSi bacSi, CapNhatHoSoBacSiRequest request) {
        bacSi.setHocVi(chuanHoa(request.hocVi()));
        bacSi.setChucVu(chuanHoa(request.chucVu()));
        bacSi.setSoNamKinhNghiem(request.soNamKinhNghiem());
        bacSi.setGioiThieuNgan(chuanHoa(request.gioiThieuNgan()));
        bacSi.setTieuSu(chuanHoa(request.tieuSu()));
        bacSi.setQuaTrinhDaoTao(gopDong(request.quaTrinhDaoTao()));
        bacSi.setQuaTrinhCongTac(gopDong(request.quaTrinhCongTac()));
        bacSi.setLinhVucKhamChua(gopDong(request.linhVucKhamChua()));
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
