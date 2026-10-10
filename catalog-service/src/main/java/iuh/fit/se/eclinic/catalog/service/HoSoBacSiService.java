package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;

/**
 * Quản trị viên xem / sửa hồ sơ giới thiệu của bác sĩ (DOC-01), kể cả bác sĩ không hiển thị công khai.
 */
public interface HoSoBacSiService {

    /** Ném KHONG_TIM_THAY nếu không có bác sĩ. */
    HoSoBacSiQuanTriResponse layHoSo(Long idBacSi);

    /** Ghi đè mọi trường của hồ sơ giới thiệu (trường bỏ trống bị xoá trắng). Ném KHONG_TIM_THAY nếu không có bác sĩ. */
    HoSoBacSiQuanTriResponse capNhatHoSo(Long idBacSi, CapNhatHoSoBacSiRequest request);

}
