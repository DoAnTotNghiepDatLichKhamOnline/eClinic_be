package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * Phiếu khám xem bằng mã ngẫu nhiên, không cần đăng nhập (BOOK-05, BOOK-06, quy tắc #8). Ai có link cũng xem được nên
 * CCCD, SĐT bị che và không có id lịch hẹn, id hồ sơ.
 *
 * @param linkPhieuKham        link trang phiếu khám trên frontend, cũng là nội dung mã QR
 * @param gioKhamDuKien        giờ bắt đầu lượt khám được xếp
 * @param ngayDat              thời điểm đặt lịch
 * @param nguoiGiamHo          null nếu lượt khám không có người giám hộ
 * @param canNguoiGiamHoDiCung true: hiển thị ghi chú "người giám hộ phải đi cùng" (BOOK-11)
 */
public record PhieuKhamResponse(String maPhieuKham, String linkPhieuKham, TrangThaiLichHen trangThai,
        Integer soThuTu, LocalDate ngay, LocalDateTime gioKhamDuKien, LocalDateTime gioBatDauKhung,
        LocalDateTime gioKetThucKhung, BacSiTomTatResponse bacSi, String tenChuyenKhoa,
        PhongKhamTomTatResponse phongKham, String lyDoKham, LocalDateTime ngayDat,
        BenhNhanPhieuKhamResponse benhNhan, NguoiGiamHoPhieuKhamResponse nguoiGiamHo,
        boolean canNguoiGiamHoDiCung) {
}
