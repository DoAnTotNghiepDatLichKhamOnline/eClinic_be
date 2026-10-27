package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

/** Bản in PDF của lịch làm việc, chữ theo ngôn ngữ của request. */
public interface PdfLichLamViecService {

    /** @param tenTep tên tệp gợi ý cho trình duyệt */
    record TepPdf(String tenTep, byte[] noiDung) {
    }

    /** Lịch làm việc của bác sĩ đang đăng nhập (A4 dọc): thông tin bác sĩ, bảng ca theo ngày, tổng số ca / số giờ. */
    TepPdf cuaToi(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay);

    /** Như {@link #cuaToi} cho 1 bác sĩ bất kỳ (quản trị viên). Ném KHONG_TIM_THAY nếu không có bác sĩ. */
    TepPdf cuaBacSi(Long idBacSi, LocalDate tuNgay, LocalDate denNgay);

    /** Lưới tuần toàn viện (A4 ngang): mỗi tuần 1 trang, mỗi dòng 1 bác sĩ, cột cuối là tổng giờ. */
    TepPdf toanVien(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa);

}
