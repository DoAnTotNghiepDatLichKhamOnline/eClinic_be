package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaCuaBacSiResponse;

/** Phần của booking-service khi 1 bác sĩ ngừng công tác (catalog-service gọi qua API nội bộ, phase 6). */
public interface NgungCongTacService {

    /**
     * Hủy mọi ca còn hoạt động chưa bắt đầu của bác sĩ, mỗi ca 1 transaction (như quản trị viên hủy từng ca: lịch hẹn
     * được giữ và đánh dấu cần đổi lịch, bệnh nhân được báo, yêu cầu đổi ca đang chờ bị đóng). Bác sĩ không được báo
     * từng ca. Ca đang diễn ra được giữ nguyên. Gọi lại nhiều lần được: lần sau chỉ hủy những ca còn sót.
     *
     * @param idTaiKhoanQuanTri quản trị viên thực hiện (ghi vào yêu cầu đổi ca bị đóng)
     */
    KetQuaHuyCaCuaBacSiResponse huyCaSapToi(Long idBacSi, Long idTaiKhoanQuanTri, String lyDo);

}
