package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.request.CapNhatHoSoRequest;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;

/**
 * Hồ sơ cá nhân của người đang đăng nhập (/api/users/me). {@code idTaiKhoan} luôn lấy từ JWT ở controller,
 * không nhận từ request.
 */
public interface HoSoCaNhanService {

    /** Thông tin tài khoản + hồ sơ bác sĩ (BAC_SI) hoặc hồ sơ bệnh nhân (BENH_NHAN), chỉ xem. */
    HoSoCaNhanResponse layHoSo(Long idTaiKhoan);

    /**
     * Sửa họ tên, số điện thoại của tài khoản. Bác sĩ chỉ sửa được số điện thoại (họ tên do quản trị viên quản lý).
     * Không đụng tới hồ sơ bệnh nhân.
     */
    HoSoCaNhanResponse capNhat(Long idTaiKhoan, CapNhatHoSoRequest request);

}
