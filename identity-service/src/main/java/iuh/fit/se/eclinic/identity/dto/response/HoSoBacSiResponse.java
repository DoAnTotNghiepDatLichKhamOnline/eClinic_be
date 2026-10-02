package iuh.fit.se.eclinic.identity.dto.response;

import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;

/**
 * Hồ sơ bác sĩ trong hồ sơ cá nhân (DOC-01): bác sĩ chỉ xem, chỉ quản trị viên được sửa.
 */
public record HoSoBacSiResponse(
        Long id,
        Long idChuyenKhoa,
        String tenChuyenKhoa,
        String hocVi,
        String soGiayPhep,
        Integer soNamKinhNghiem,
        String tieuSu,
        TrangThaiBacSi trangThai) {
}
