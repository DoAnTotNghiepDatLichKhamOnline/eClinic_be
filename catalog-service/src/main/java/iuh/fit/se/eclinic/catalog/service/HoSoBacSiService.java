package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoCuaToiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;

/**
 * Quản trị viên xem / sửa hồ sơ giới thiệu của bác sĩ (DOC-01), kể cả bác sĩ không hiển thị công khai; bác sĩ sửa phần
 * của mình qua {@link HoSoCuaBacSiService}. Mỗi lần lưu tăng phiên bản hồ sơ.
 */
public interface HoSoBacSiService {

    /** Ném KHONG_TIM_THAY nếu không có bác sĩ. */
    HoSoBacSiQuanTriResponse layHoSo(Long idBacSi);

    /**
     * Quản trị viên ghi đè mọi trường của hồ sơ giới thiệu (trường bỏ trống bị xoá trắng). Ném KHONG_TIM_THAY nếu không
     * có bác sĩ, DU_LIEU_DA_THAY_DOI (409) nếu phiên bản đã cũ, DU_LIEU_KHONG_HOP_LE (400) nếu năm bắt đầu hành nghề
     * sau năm nay.
     */
    HoSoBacSiQuanTriResponse capNhatHoSo(Long idBacSi, CapNhatHoSoBacSiRequest request);

    /** Bác sĩ tự sửa phần hồ sơ của mình (mục khác giữ nguyên). Lỗi như {@link #capNhatHoSo}. */
    HoSoBacSiQuanTriResponse capNhatHoSoCuaToi(Long idBacSi, CapNhatHoSoCuaToiRequest request);

}
