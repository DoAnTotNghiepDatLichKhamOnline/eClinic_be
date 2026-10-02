package iuh.fit.se.eclinic.identity.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.identity.dto.response.HoSoBacSiResponse;
import iuh.fit.se.eclinic.identity.dto.response.HoSoBenhNhanResponse;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;

/**
 * Chuyển TaiKhoan (+ hồ sơ bác sĩ / hồ sơ bệnh nhân nếu có) -> hồ sơ cá nhân. Viết tay, không bao giờ đưa
 * matKhauHash hay googleId ra ngoài (chỉ báo có / không).
 */
@Component
public class HoSoCaNhanMapper {

    /** @param bacSi null nếu không có; @param hoSoBenhNhan null nếu không có */
    public HoSoCaNhanResponse toResponse(TaiKhoan taiKhoan, BacSi bacSi, HoSoBenhNhan hoSoBenhNhan) {
        return new HoSoCaNhanResponse(taiKhoan.getId(), taiKhoan.getHoTen(), taiKhoan.getEmail(),
                taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), taiKhoan.getVaiTro(), taiKhoan.getTrangThai(),
                taiKhoan.getMatKhauHash() != null, taiKhoan.getGoogleId() != null, taiKhoan.getNgayTao(),
                bacSi == null ? null : toResponse(bacSi),
                hoSoBenhNhan == null ? null : toResponse(hoSoBenhNhan));
    }

    /** Dùng chung với TaiKhoanQuanTriMapper. {@code bacSi.chuyenKhoa} phải đọc được (đã nạp hoặc còn trong transaction). */
    public HoSoBacSiResponse toResponse(BacSi bacSi) {
        return new HoSoBacSiResponse(bacSi.getId(), bacSi.getChuyenKhoa().getId(),
                bacSi.getChuyenKhoa().getTenChuyenKhoa(), bacSi.getHocVi(), bacSi.getSoGiayPhep(),
                bacSi.getSoNamKinhNghiem(), bacSi.getTieuSu(), bacSi.getTrangThai());
    }

    private HoSoBenhNhanResponse toResponse(HoSoBenhNhan hoSo) {
        // Chờ quản trị viên xác minh (SĐT không khớp hồ sơ): chỉ báo trạng thái, chưa cho xem thông tin của hồ sơ
        if (hoSo.getTrangThaiLienKet() != TrangThaiLienKet.DA_LIEN_KET) {
            return new HoSoBenhNhanResponse(null, hoSo.getTrangThaiLienKet(), null, null, null, null, null, null, null,
                    null);
        }
        return new HoSoBenhNhanResponse(hoSo.getId(), hoSo.getTrangThaiLienKet(), hoSo.getCccd(), hoSo.getHoTen(),
                hoSo.getNgaySinh(), hoSo.getGioiTinh(), hoSo.getSoDienThoai(), hoSo.getDiaChi(),
                hoSo.getSoBaoHiemYTe(), hoSo.getTienSuBenhLy());
    }

}
