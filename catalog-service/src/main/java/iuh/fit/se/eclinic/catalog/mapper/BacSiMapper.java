package iuh.fit.se.eclinic.catalog.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.common.entity.catalog.AnhBacSi;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

/**
 * Chuyển entity bác sĩ sang DTO. Họ tên, ảnh đại diện lấy từ tài khoản của bác sĩ.
 * <p>
 * 3 mục danh sách (đào tạo, công tác, lĩnh vực khám chữa) lưu trong DB là văn bản mỗi dòng 1 ý, DTO là mảng chuỗi.
 */
@Component
public class BacSiMapper {

    public BacSiResponse toResponse(BacSi bacSi) {
        return new BacSiResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(), bacSi.getTaiKhoan().getAnhDaiDien(),
                bacSi.getHocVi(), bacSi.getChucVu(), bacSi.getSoNamKinhNghiem(), bacSi.getGioiThieuNgan(),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa());
    }

    public BacSiChiTietResponse toChiTietResponse(BacSi bacSi, List<AnhBacSi> danhSachAnh) {
        return new BacSiChiTietResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(),
                bacSi.getTaiKhoan().getAnhDaiDien(), bacSi.getHocVi(), bacSi.getChucVu(), bacSi.getSoNamKinhNghiem(),
                bacSi.getGioiThieuNgan(), bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa(),
                bacSi.getTieuSu(), tachDong(bacSi.getQuaTrinhDaoTao()), tachDong(bacSi.getQuaTrinhCongTac()),
                tachDong(bacSi.getLinhVucKhamChua()), toAnhResponse(danhSachAnh));
    }

    public HoSoBacSiQuanTriResponse toQuanTriResponse(BacSi bacSi, List<AnhBacSi> danhSachAnh) {
        return new HoSoBacSiQuanTriResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(),
                bacSi.getTaiKhoan().getAnhDaiDien(), bacSi.getSoGiayPhep(), bacSi.getTrangThai(), bacSi.getHocVi(),
                bacSi.getChucVu(), bacSi.getSoNamKinhNghiem(), bacSi.getGioiThieuNgan(),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa(), bacSi.getTieuSu(),
                tachDong(bacSi.getQuaTrinhDaoTao()), tachDong(bacSi.getQuaTrinhCongTac()),
                tachDong(bacSi.getLinhVucKhamChua()), toAnhResponse(danhSachAnh));
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
