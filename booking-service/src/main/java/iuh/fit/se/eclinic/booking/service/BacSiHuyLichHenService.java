package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;

/** Bác sĩ hủy 1 lịch hẹn đã xác nhận của CHÍNH MÌNH (vd bệnh nhân đặt nhầm chuyên khoa). */
public interface BacSiHuyLichHenService {

    /** Hủy lịch, ghi lý do, trả lượt khám về CON_TRONG và báo bệnh nhân qua ứng dụng và email. */
    LichHenTrongCaResponse huy(Long idTaiKhoanBacSi, Long idLichHen, String lyDo);

}
