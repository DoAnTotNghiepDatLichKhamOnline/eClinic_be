package iuh.fit.se.eclinic.identity.dto.response;

import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;

/** Kết quả của các API nội bộ về tài khoản bác sĩ. */
public record TaiKhoanNoiBoResponse(Long id, TrangThaiTaiKhoan trangThai) {
}
