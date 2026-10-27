package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.DanhGiaRequest;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaBacSiResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanDanhGiaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

/** Bệnh nhân đánh giá lượt khám đã hoàn thành. */
public interface DanhGiaService {

    /** Bệnh nhân đánh giá 1 lượt khám đã khám xong mà mình được xem kết quả. */
    DanhGiaCuaToiResponse gui(Long idTaiKhoan, String maPhieuKham, DanhGiaRequest request);

    /** Thay số sao và nhận xét trong thời hạn sửa (cấu hình soNgayDuocSuaDanhGia) kể từ lúc gửi. */
    DanhGiaCuaToiResponse sua(Long idTaiKhoan, String maPhieuKham, DanhGiaRequest request);

    /** Điểm trung bình, số đánh giá và phân bố theo sao của bác sĩ đang đăng nhập. */
    TongQuanDanhGiaResponse tongQuanCuaBacSi(Long idTaiKhoanBacSi);

    /** Đánh giá bác sĩ đang đăng nhập nhận được, mới nhất trước, không có tên bệnh nhân. */
    TrangDuLieu<DanhGiaCuaBacSiResponse> cuaBacSi(Long idTaiKhoanBacSi, int trang, int kichThuoc);

    /** Đánh giá cho quản trị viên, mới nhất trước. */
    TrangDuLieu<DanhGiaQuanTriResponse> choQuanTri(Long idBacSi, Integer soSaoToiDa, int trang, int kichThuoc);
}
