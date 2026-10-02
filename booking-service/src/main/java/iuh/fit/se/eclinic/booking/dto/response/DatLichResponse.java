package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * Kết quả đặt lịch (BOOK-05). Không trả id lịch hẹn: phiếu khám chỉ tra bằng {@code maPhieuKham} (quy tắc #8).
 *
 * @param maPhieuKham      chuỗi ngẫu nhiên dùng cho link / QR phiếu khám
 * @param linkPhieuKham    link trang phiếu khám trên frontend, cũng là nội dung mã QR
 * @param soThuTu          số thứ tự khám theo phòng khám + ngày
 * @param gioKhamDuKien    giờ bắt đầu lượt khám được xếp (giờ đầu khung + (vị trí - 1) x t)
 * @param hoTenNguoiGiamHo null nếu lượt khám không có người giám hộ
 * @param luuVaoTaiKhoan   true nếu lịch được đặt khi đã đăng nhập nên có trong "lịch hẹn của tôi"; false (khách đặt) thì
 *                         chỉ xem lại được bằng link phiếu khám
 */
public record DatLichResponse(String maPhieuKham, String linkPhieuKham, Integer soThuTu, LocalDate ngay,
        LocalDateTime gioKhamDuKien, LocalDateTime gioBatDauKhung, LocalDateTime gioKetThucKhung,
        TrangThaiLichHen trangThai, BacSiTomTatResponse bacSi, PhongKhamTomTatResponse phongKham,
        String hoTenBenhNhan, String hoTenNguoiGiamHo, boolean luuVaoTaiKhoan) {
}
