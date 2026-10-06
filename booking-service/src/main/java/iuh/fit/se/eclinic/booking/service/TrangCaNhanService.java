package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.TrangCaNhanResponse;

public interface TrangCaNhanService {

    /** Số lịch sắp tới của mỗi bên trả kèm trang cá nhân. */
    int SO_LICH_SAP_TOI = 5;

    /** Số lượt đã khám gần nhất trả kèm trang cá nhân. */
    int SO_LAN_KHAM_GAN_DAY = 5;

    /**
     * Hồ sơ bệnh nhân của tài khoản, người thân đã lưu, các lịch sắp tới gần nhất tách "của tôi" / "của người khác" và
     * số lịch mỗi loại. Ném các mã của {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    TrangCaNhanResponse cuaToi(Long idTaiKhoan);

}
