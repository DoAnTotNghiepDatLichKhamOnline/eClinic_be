package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.DanhGiaRequest;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaBacSiResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanDanhGiaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

/**
 * Bệnh nhân đánh giá lượt khám đã hoàn thành. Người được đánh giá = người được xem kết quả khám của lượt đó: chủ tài
 * khoản là người khám, hoặc tài khoản đã đặt lịch. Mỗi lịch hẹn 1 đánh giá, không xoá.
 */
public interface DanhGiaService {

    /**
     * Ném KHONG_TIM_THAY (tài khoản không thấy lịch hẹn này), KHONG_CO_QUYEN (thấy lịch hẹn nhưng không được xem kết
     * quả), LICH_HEN_CHUA_KHAM_XONG, DA_DANH_GIA.
     */
    DanhGiaCuaToiResponse gui(Long idTaiKhoan, String maPhieuKham, DanhGiaRequest request);

    /**
     * Thay số sao và nhận xét trong thời hạn sửa (app.danh-gia.so-ngay-duoc-sua) kể từ lúc gửi.
     * <p>
     * Ném KHONG_TIM_THAY (không thấy lịch hẹn, hoặc lịch hẹn chưa có đánh giá), KHONG_CO_QUYEN, HET_HAN_SUA_DANH_GIA.
     */
    DanhGiaCuaToiResponse sua(Long idTaiKhoan, String maPhieuKham, DanhGiaRequest request);

    /** Điểm trung bình, số đánh giá và phân bố theo sao của bác sĩ đang đăng nhập. */
    TongQuanDanhGiaResponse tongQuanCuaBacSi(Long idTaiKhoanBacSi);

    /** Đánh giá bác sĩ đang đăng nhập nhận được, mới nhất trước, không có tên bệnh nhân. */
    TrangDuLieu<DanhGiaCuaBacSiResponse> cuaBacSi(Long idTaiKhoanBacSi, int trang, int kichThuoc);

    /**
     * Đánh giá cho quản trị viên, mới nhất trước.
     *
     * @param idBacSi    null = mọi bác sĩ
     * @param soSaoToiDa null = mọi mức sao
     */
    TrangDuLieu<DanhGiaQuanTriResponse> choQuanTri(Long idBacSi, Integer soSaoToiDa, int trang, int kichThuoc);
}
