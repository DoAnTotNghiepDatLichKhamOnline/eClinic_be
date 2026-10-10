package iuh.fit.se.eclinic.medical.service;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

public interface BacSiDangNhapService {

    /**
     * Bác sĩ của tài khoản đang đăng nhập, sau khi kiểm tra lại trong DB rằng tài khoản còn hoạt động. Ném
     * CHUA_DANG_NHAP (tài khoản không còn), TAI_KHOAN_BI_VO_HIEU_HOA, TAI_KHOAN_CHUA_XAC_THUC, KHONG_CO_QUYEN (không
     * phải bác sĩ hoặc chưa có hồ sơ bác sĩ).
     */
    BacSi layBacSiDangHoatDong(Long idTaiKhoan);

}
