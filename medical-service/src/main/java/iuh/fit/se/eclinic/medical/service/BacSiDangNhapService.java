package iuh.fit.se.eclinic.medical.service;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

public interface BacSiDangNhapService {

    /** Bác sĩ của tài khoản đang đăng nhập, sau khi kiểm tra lại trong DB rằng tài khoản còn hoạt động. */
    BacSi layBacSiDangHoatDong(Long idTaiKhoan);

}
