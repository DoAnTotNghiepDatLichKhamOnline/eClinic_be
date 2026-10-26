package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.TongQuanBacSiResponse;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanQuanTriResponse;

/**
 * Số liệu của 2 màn hình Dashboard, mỗi màn hình 1 lần đọc, đếm trực tiếp lúc gọi. Định nghĩa từng số ở 2 response.
 */
public interface TongQuanService {

    TongQuanBacSiResponse cuaBacSi(Long idTaiKhoanBacSi);

    TongQuanQuanTriResponse cuaQuanTri();
}
