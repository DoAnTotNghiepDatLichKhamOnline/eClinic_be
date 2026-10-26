package iuh.fit.se.eclinic.identity.mapper;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.identity.dto.response.ChiTietTaiKhoanResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanQuanTriResponse;
import lombok.RequiredArgsConstructor;

/**
 * Chuyển TaiKhoan -> dữ liệu cho màn hình quản lý tài khoản của quản trị viên. Viết tay, không bao giờ đưa matKhauHash
 * hay googleId ra ngoài (chỉ báo có / không), và không đưa dữ liệu y tế của hồ sơ bệnh nhân.
 */
@Component
@RequiredArgsConstructor
public class TaiKhoanQuanTriMapper {

    private final HoSoCaNhanMapper hoSoCaNhanMapper;

    /** @param ngaySinh ngày sinh từ hồ sơ bệnh nhân đã liên kết; null nếu không có */
    public TaiKhoanQuanTriResponse toResponse(TaiKhoan taiKhoan, LocalDate ngaySinh) {
        return new TaiKhoanQuanTriResponse(taiKhoan.getId(), taiKhoan.getHoTen(), taiKhoan.getEmail(),
                taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), taiKhoan.getVaiTro(), taiKhoan.getTrangThai(),
                taiKhoan.getLyDoVoHieuHoa(), ngaySinh, taiKhoan.getNgayTao());
    }

    /** @param bacSi null nếu không có; @param hoSoBenhNhan null nếu không có */
    public ChiTietTaiKhoanResponse toChiTiet(TaiKhoan taiKhoan, BacSi bacSi, HoSoBenhNhan hoSoBenhNhan) {
        return new ChiTietTaiKhoanResponse(taiKhoan.getId(), taiKhoan.getHoTen(), taiKhoan.getEmail(),
                taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), taiKhoan.getVaiTro(), taiKhoan.getTrangThai(),
                taiKhoan.getLyDoVoHieuHoa(), ngaySinh(hoSoBenhNhan), taiKhoan.getNgayTao(),
                taiKhoan.getMatKhauHash() != null, taiKhoan.getGoogleId() != null, taiKhoan.getNgayCapNhat(),
                hoSoBenhNhan == null ? null : hoSoBenhNhan.getTrangThaiLienKet(),
                bacSi == null ? null : hoSoCaNhanMapper.toResponse(bacSi));
    }

    /** Hồ sơ đang chờ quản trị viên xác minh thì chưa coi là của tài khoản: không lấy ngày sinh. */
    private static LocalDate ngaySinh(HoSoBenhNhan hoSoBenhNhan) {
        return hoSoBenhNhan != null && hoSoBenhNhan.getTrangThaiLienKet() == TrangThaiLienKet.DA_LIEN_KET
                ? hoSoBenhNhan.getNgaySinh()
                : null;
    }

}
