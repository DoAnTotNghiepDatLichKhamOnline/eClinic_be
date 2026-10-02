package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;

/**
 * BOOK-05, BOOK-06: phiếu khám xem bằng mã ngẫu nhiên (quy tắc #8), không cần đăng nhập. Mã sai dạng hoặc không tồn tại
 * đều ném {@code KHONG_TIM_THAY}.
 */
public interface PhieuKhamService {

    PhieuKhamResponse xem(String maPhieuKham);

    /**
     * Ảnh PNG mã QR chứa link phiếu khám.
     *
     * @param kichThuoc cạnh ảnh (pixel); null = mặc định {@code app.phieu-kham.kich-thuoc-qr}
     */
    byte[] taoQr(String maPhieuKham, Integer kichThuoc);

}
