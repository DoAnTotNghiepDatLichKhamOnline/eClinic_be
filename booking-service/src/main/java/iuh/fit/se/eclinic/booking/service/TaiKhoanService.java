package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

public interface TaiKhoanService {

    /** Tài khoản bệnh nhân đang hoạt động, đọc từ DB vì JWT của tài khoản vừa bị khoá có thể chưa hết hạn. */
    TaiKhoan layBenhNhanDangHoatDong(Long id);

    /** Bác sĩ của tài khoản đang đăng nhập, đọc từ DB như {@link #layBenhNhanDangHoatDong}. */
    BacSi layBacSiDangHoatDong(Long idTaiKhoan);

}
