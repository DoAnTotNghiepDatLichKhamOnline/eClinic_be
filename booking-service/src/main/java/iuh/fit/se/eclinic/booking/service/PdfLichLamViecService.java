package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

/**
 * Bản in PDF của lịch làm việc, chữ theo ngôn ngữ của request. Chỉ có ca còn hoạt động (ca đã huỷ không in). Khoảng
 * ngày tối đa 42 ngày như các API xem lịch (KhoangNgay).
 */
public interface PdfLichLamViecService {

    /** @param tenTep tên tệp gợi ý cho trình duyệt */
    record TepPdf(String tenTep, byte[] noiDung) {
    }

    /**
     * Lịch làm việc của bác sĩ đang đăng nhập (A4 dọc): thông tin bác sĩ, bảng ca theo ngày, tổng số ca / số giờ. Không
     * có ca thì vẫn có tệp, ghi rõ không có ca. Lỗi tài khoản như {@link TaiKhoanService#layBacSiDangHoatDong}.
     */
    TepPdf cuaToi(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay);

    /** Như {@link #cuaToi} cho 1 bác sĩ bất kỳ (quản trị viên). Ném KHONG_TIM_THAY nếu không có bác sĩ. */
    TepPdf cuaBacSi(Long idBacSi, LocalDate tuNgay, LocalDate denNgay);

    /**
     * Lưới tuần toàn viện (A4 ngang): mỗi tuần thứ Hai - Chủ nhật có ngày nằm trong [tuNgay, denNgay] là 1 trang, đủ 7
     * ngày; mỗi dòng 1 bác sĩ có ca trong tuần đó (theo chuyên khoa), cột cuối là tổng giờ trong tuần.
     *
     * @param idChuyenKhoa null = mọi chuyên khoa
     */
    TepPdf toanVien(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa);

}
