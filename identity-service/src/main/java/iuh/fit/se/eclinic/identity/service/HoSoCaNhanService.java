package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatHoSoRequest;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;

/** Hồ sơ cá nhân của người đang đăng nhập (/api/users/me). */
public interface HoSoCaNhanService {

    /** Thông tin tài khoản + hồ sơ bác sĩ (BAC_SI) hoặc hồ sơ bệnh nhân (BENH_NHAN), chỉ xem. */
    HoSoCaNhanResponse layHoSo(Long idTaiKhoan);

    /** Sửa họ tên, số điện thoại của tài khoản. */
    HoSoCaNhanResponse capNhat(Long idTaiKhoan, CapNhatHoSoRequest request);

    /** Lưu ngôn ngữ ưa dùng của tài khoản (dùng cho email, nhắc lịch). */
    HoSoCaNhanResponse doiNgonNgu(Long idTaiKhoan, NgonNgu ngonNgu);

}
