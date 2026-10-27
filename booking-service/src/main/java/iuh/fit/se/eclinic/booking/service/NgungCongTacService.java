package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaCuaBacSiResponse;

/** Phần của booking-service khi 1 bác sĩ ngừng công tác (catalog-service gọi qua API nội bộ). */
public interface NgungCongTacService {

    /** Hủy mọi ca chưa bắt đầu của bác sĩ, mỗi ca 1 transaction, như quản trị viên hủy từng ca. */
    KetQuaHuyCaCuaBacSiResponse huyCaSapToi(Long idBacSi, Long idTaiKhoanQuanTri, String lyDo);

}
