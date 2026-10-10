package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

public interface LichLamViecService {

    LichLamViec layTheoId(Long id);

    List<LichLamViec> timTheoBacSi(Long bacSiId, LocalDate tuNgay, LocalDate denNgay);

    /**
     * Ném LoiNghiepVu(TRUNG_LICH_LAM_VIEC) nếu ca [gioBatDau, gioKetThuc) trong ngày ngayLamViec
     * chồng giờ với ca khác (còn hoạt động) của cùng bác sĩ hoặc cùng phòng khám.
     *
     * @param excludeLichLamViecId id ca đang sửa, null khi tạo mới
     */
    void kiemTraKhongTrungLich(Long bacSiId, Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeLichLamViecId);

    /** Số ngày tối đa của 1 lần xem lịch làm việc (lưới tháng 6 tuần). */
    int SO_NGAY_XEM_TOI_DA = KhoangNgay.SO_NGAY_TOI_DA;

    /**
     * Lịch làm việc của bác sĩ đang đăng nhập trong [tuNgay, denNgay], kể cả ca đã huỷ, theo ngày rồi giờ bắt đầu.
     * Ném DU_LIEU_KHONG_HOP_LE nếu denNgay trước tuNgay hoặc khoảng dài hơn {@link #SO_NGAY_XEM_TOI_DA} ngày; lỗi tài
     * khoản như {@link TaiKhoanService#layBacSiDangHoatDong}.
     */
    List<CaLamViecResponse> lichCuaBacSi(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay);

    /** Lịch làm việc toàn viện cho quản trị viên, như {@link #lichCuaBacSi}; bộ lọc null = không lọc. */
    List<CaLamViecResponse> lichToanVien(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi,
            Long idPhongKham);

    /**
     * Lịch hẹn (mọi trạng thái) trong 1 ngày của bác sĩ đang đăng nhập, theo giờ khám; các bộ lọc null / rỗng = không
     * lọc.
     *
     * @param ngay          null = hôm nay
     * @param idLichLamViec chỉ lịch hẹn của ca này (số thứ tự tính theo phòng khám + ngày nên 2 ca khác phòng có thể
     *                      trùng số)
     * @param soThuTu       đúng số thứ tự này
     * @param tuKhoa        một phần họ tên bệnh nhân (trong hồ sơ hoặc do người đặt nhập), không xét dấu, hoa/thường
     */
    List<LichHenTrongCaResponse> lichHenCuaBacSiTheoNgay(Long idTaiKhoan, LocalDate ngay, Long idLichLamViec,
            Integer soThuTu, String tuKhoa);

    /**
     * Lịch hẹn (mọi trạng thái) của bác sĩ đang đăng nhập có ngày khám trong [tuNgay, denNgay], theo giờ khám: cho màn
     * hình lịch của bác sĩ. Giới hạn khoảng ngày và lỗi như {@link #lichCuaBacSi}.
     */
    List<LichHenTrongCaResponse> lichHenCuaBacSiTrongKhoang(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay);

    /**
     * Tra 1 lịch hẹn theo mã in trên phiếu khám: mã tra cứu ngắn (ECL-...), mã phiếu khám, hoặc cả link phiếu khám
     * mà máy quét QR trả về (xem {@link LichHenService#timTheoMa}).
     * Ném KHONG_TIM_THAY nếu không có, hoặc lịch hẹn không phải của bác sĩ đang đăng nhập (cùng 1 thông điệp).
     *
     * @param idTaiKhoanBacSi tài khoản bác sĩ đang tra; null = quản trị viên tra (mọi bác sĩ)
     */
    LichHenTrongCaResponse traCuuLichHen(Long idTaiKhoanBacSi, String ma);

    /** Lịch hẹn (mọi trạng thái) của 1 ca, theo giờ khám, cho quản trị viên. Ném KHONG_TIM_THAY nếu không có ca. */
    List<LichHenTrongCaResponse> lichHenCuaCa(Long idLichLamViec);

}
