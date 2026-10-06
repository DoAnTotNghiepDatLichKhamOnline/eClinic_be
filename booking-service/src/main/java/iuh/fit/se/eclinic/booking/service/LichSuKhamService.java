package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.PhamViLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LanKhamCuaToiResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

public interface LichSuKhamService {

    /**
     * Lịch sử khám của tài khoản: các lượt đã khám xong mà tài khoản được xem kết quả, giờ khám muộn nhất trước. Được
     * xem khi người khám là chủ tài khoản (hồ sơ đã liên kết, bất kể ai đặt) hoặc chính tài khoản này đã đặt lịch.
     * {@code phamVi} tách "của tôi" / "của người khác" như ở lịch hẹn của tôi.
     * <p>
     * Ném các mã của {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    TrangDuLieu<LanKhamCuaToiResponse> cuaToi(Long idTaiKhoan, PhamViLichHen phamVi, int trang, int kichThuoc);

}
