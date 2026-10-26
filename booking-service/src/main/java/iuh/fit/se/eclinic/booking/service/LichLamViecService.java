package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.GioLamTheoTuanResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichSuCaResponse;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

public interface LichLamViecService {

    LichLamViec layTheoId(Long id);

    List<LichLamViec> timTheoBacSi(Long bacSiId, LocalDate tuNgay, LocalDate denNgay);

    /** Ném TRUNG_LICH_LAM_VIEC nếu ca chồng giờ với ca khác còn hoạt động của cùng bác sĩ hoặc cùng phòng. */
    void kiemTraKhongTrungLich(Long bacSiId, Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeLichLamViecId);

    /** Số ngày tối đa của 1 lần xem lịch làm việc (lưới tháng 6 tuần). */
    int SO_NGAY_XEM_TOI_DA = KhoangNgay.SO_NGAY_TOI_DA;

    /** Lịch làm việc của bác sĩ đang đăng nhập trong [tuNgay, denNgay], kể cả ca đã huỷ, theo ngày rồi giờ bắt đầu. */
    List<CaLamViecResponse> lichCuaBacSi(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay);

    /** Lịch làm việc toàn viện cho quản trị viên, như {@link #lichCuaBacSi}; bộ lọc null = không lọc. */
    List<CaLamViecResponse> lichToanVien(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi,
            Long idPhongKham);

    /** Lịch hẹn trong 1 ngày của bác sĩ đang đăng nhập, theo giờ khám; bộ lọc null là không lọc. */
    List<LichHenTrongCaResponse> lichHenCuaBacSiTheoNgay(Long idTaiKhoan, LocalDate ngay, Long idLichLamViec,
            Integer soThuTu, String tuKhoa);

    /** Lịch hẹn của bác sĩ đang đăng nhập trong khoảng ngày, cho màn hình lịch của bác sĩ. */
    List<LichHenTrongCaResponse> lichHenCuaBacSiTrongKhoang(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay);

    /** Tra 1 lịch hẹn theo mã tra cứu, mã phiếu khám hoặc link phiếu khám quét từ QR. */
    LichHenTrongCaResponse traCuuLichHen(Long idTaiKhoanBacSi, String ma);

    /** Lịch hẹn (mọi trạng thái) của 1 ca, theo giờ khám, cho quản trị viên. Ném KHONG_TIM_THAY nếu không có ca. */
    List<LichHenTrongCaResponse> lichHenCuaCa(Long idLichLamViec);

    /** Giờ làm theo tuần (thứ Hai - Chủ nhật) của các bác sĩ, cho quản trị viên. */
    GioLamTheoTuanResponse gioLamTheoTuan(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi);

    /** Lịch sử xếp / sửa / hủy của 1 ca, cũ nhất trước, cho quản trị viên. Ném KHONG_TIM_THAY nếu không có ca. */
    List<LichSuCaResponse> lichSuCa(Long idLichLamViec);

}
