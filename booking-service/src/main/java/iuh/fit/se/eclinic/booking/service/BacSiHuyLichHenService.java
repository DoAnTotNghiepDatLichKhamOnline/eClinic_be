package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;

/**
 * Bác sĩ hủy 1 lịch hẹn đã xác nhận của CHÍNH MÌNH (vd bệnh nhân đặt nhầm chuyên khoa). Lịch hẹn được xác nhận ngay khi
 * đặt nên bác sĩ không duyệt từng lịch; đây là cách duy nhất để bác sĩ bỏ 1 lịch hẹn lẻ (nghỉ cả ca thì gửi yêu cầu xin
 * nghỉ). Lịch hẹn không có hoặc của bác sĩ khác đều ném KHONG_TIM_THAY với cùng 1 thông điệp. Lỗi tài khoản như
 * {@link TaiKhoanService#layBacSiDangHoatDong}.
 */
public interface BacSiHuyLichHenService {

    /**
     * DA_XAC_NHAN -> BI_TU_CHOI, ghi lý do (bệnh nhân đọc được trên phiếu khám), trả lượt khám về CON_TRONG để người khác
     * đặt được, báo trong ứng dụng cho tài khoản phía bệnh nhân và gửi email tới địa chỉ liên hệ của lịch hẹn.
     * <p>
     * Ném LICH_HEN_KHONG_HUY_DOI_DUOC nếu lịch không còn DA_XAC_NHAN (đã khám, đã hủy; kể cả khi gọi lặp lại);
     * LICH_HEN_CAN_DOI_LICH nếu ca đã bị hủy và lịch đang chờ bệnh nhân đổi; LICH_HEN_DA_QUA_GIO nếu lượt khám đã bắt đầu.
     */
    LichHenTrongCaResponse huy(Long idTaiKhoanBacSi, Long idLichHen, String lyDo);

}
