package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.PhamViLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LanKhamCuaToiResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

public interface LichSuKhamService {

    /** Lịch sử khám của tài khoản: các lượt đã khám xong mà tài khoản được xem kết quả, giờ khám muộn nhất trước. */
    TrangDuLieu<LanKhamCuaToiResponse> cuaToi(Long idTaiKhoan, PhamViLichHen phamVi, int trang, int kichThuoc);

}
