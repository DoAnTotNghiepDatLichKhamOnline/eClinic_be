package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

public interface TaiKhoanService {

    /**
     * Tài khoản bệnh nhân đang hoạt động, đọc từ DB chứ không tin riêng JWT: tài khoản vừa bị vô hiệu hoá vẫn có thể còn
     * access token chưa hết hạn. Mã lỗi giống identity-service.
     * <p>
     * Ném: CHUA_DANG_NHAP (không còn tài khoản), TAI_KHOAN_BI_VO_HIEU_HOA, TAI_KHOAN_CHUA_XAC_THUC, KHONG_CO_QUYEN
     * (không phải bệnh nhân).
     */
    TaiKhoan layBenhNhanDangHoatDong(Long id);

}
