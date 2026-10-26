package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse;

/**
 * Xuất / nhập lịch làm việc bằng tệp Excel (.xlsx) cho quản trị viên. Tệp mẫu, tệp xuất và tệp nhập dùng chung 1 bố
 * cục theo vị trí cột ở trang đầu tiên: A mã bác sĩ (BS0001), B ngày, C giờ bắt đầu, D giờ kết thúc, E mã phòng, F số
 * lượt mỗi giờ (N), G số phút mỗi lượt (t). Dòng 1 là tiêu đề; cột sau G chỉ để xem, khi nhập được bỏ qua. Tiêu đề,
 * tên trang theo ngôn ngữ của request; nhập không phụ thuộc ngôn ngữ vì đọc theo vị trí.
 */
public interface ExcelLichLamViecService {

    /**
     * Các ca CÒN HOẠT ĐỘNG trong khoảng ngày (tối đa 42 ngày, như lịch toàn viện) theo bộ lọc, kèm cột thông tin (họ
     * tên, chuyên khoa, phòng, tầng, số lượt đã đặt / tổng). Ca đã hủy không xuất: nhập lại tệp sẽ thành ca hoạt động.
     */
    byte[] xuat(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi, Long idPhongKham);

    /**
     * Tệp mẫu: trang ca chỉ có tiêu đề, trang hướng dẫn, danh sách bác sĩ đang công tác và phòng đang hoạt động; cột
     * mã bác sĩ / mã phòng có danh sách chọn lấy từ 2 trang đó.
     */
    byte[] mau();

    /**
     * Đọc tệp rồi kiểm tra từng ô (mã bác sĩ, ngày, giờ, số), trùng giờ giữa các dòng trong tệp, rồi các quy tắc xếp ca
     * với DB ({@link QuanLyCaService#nhapCa}). Tất cả hoặc không.
     * <p>
     * 400 DU_LIEU_KHONG_HOP_LE khi tệp không mở được dưới dạng .xlsx, không có dòng ca nào, quá số dòng tối đa, hoặc
     * {@code luu} mà tệp còn lỗi.
     *
     * @param luu false = xem trước, không lưu gì
     */
    KetQuaNhapExcelResponse nhap(Long idTaiKhoanQuanTri, MultipartFile tep, boolean luu);

}
