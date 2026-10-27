package iuh.fit.se.eclinic.identity.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.identity.dto.response.HoSoBacSiResponse;
import iuh.fit.se.eclinic.identity.dto.response.HoSoBenhNhanResponse;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;

/** Chuyển TaiKhoan (+ hồ sơ bác sĩ / hồ sơ bệnh nhân nếu có) -> hồ sơ cá nhân. */
@Component
public class HoSoCaNhanMapper {

    /** Hồ sơ cá nhân; {@code bacSi}, {@code hoSoBenhNhan} null nếu không có. */
    public HoSoCaNhanResponse toResponse(TaiKhoan taiKhoan, BacSi bacSi, HoSoBenhNhan hoSoBenhNhan) {
        return new HoSoCaNhanResponse(taiKhoan.getId(), taiKhoan.getHoTen(), taiKhoan.getEmail(),
                taiKhoan.getSoDienThoai(), taiKhoan.getAnhDaiDien(), taiKhoan.getVaiTro(), taiKhoan.getTrangThai(),
                taiKhoan.getNgonNgu(),
                taiKhoan.getMatKhauHash() != null, taiKhoan.getGoogleId() != null, taiKhoan.getNgayTao(),
                bacSi == null ? null : toResponse(bacSi),
                hoSoBenhNhan == null ? null : toResponse(hoSoBenhNhan));
    }

    /** Dùng chung với TaiKhoanQuanTriMapper; {@code bacSi.chuyenKhoa} phải đọc được. */
    public HoSoBacSiResponse toResponse(BacSi bacSi) {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        return new HoSoBacSiResponse(bacSi.getId(), bacSi.getChuyenKhoa().getId(),
                bacSi.getChuyenKhoa().ten(ngonNgu), bacSi.getHocVi(), bacSi.getSoGiayPhep(),
                bacSi.soNamKinhNghiem(), bacSi.tieuSu(ngonNgu), bacSi.getTrangThai());
    }

    private HoSoBenhNhanResponse toResponse(HoSoBenhNhan hoSo) {
        // Chờ quản trị viên xác minh: chỉ báo trạng thái, chưa cho xem thông tin của hồ sơ
        if (hoSo.getTrangThaiLienKet() != TrangThaiLienKet.DA_LIEN_KET) {
            return new HoSoBenhNhanResponse(null, hoSo.getTrangThaiLienKet(), null, null, null, null, null, null, null,
                    null);
        }
        return new HoSoBenhNhanResponse(hoSo.getId(), hoSo.getTrangThaiLienKet(), hoSo.getCccd(), hoSo.getHoTen(),
                hoSo.getNgaySinh(), hoSo.getGioiTinh(), hoSo.getSoDienThoai(), hoSo.getDiaChi(),
                hoSo.getSoBaoHiemYTe(), hoSo.getTienSuBenhLy());
    }

}
