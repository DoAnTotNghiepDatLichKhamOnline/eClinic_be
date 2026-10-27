package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.TrangCaNhanResponse;

public interface TrangCaNhanService {

    /** Số lịch sắp tới của mỗi bên trả kèm trang cá nhân. */
    int SO_LICH_SAP_TOI = 5;

    /** Số lượt đã khám gần nhất trả kèm trang cá nhân. */
    int SO_LAN_KHAM_GAN_DAY = 5;

    /** Hồ sơ, người thân đã lưu và các lịch sắp tới của tài khoản, tách của tôi và của người khác. */
    TrangCaNhanResponse cuaToi(Long idTaiKhoan);

}
