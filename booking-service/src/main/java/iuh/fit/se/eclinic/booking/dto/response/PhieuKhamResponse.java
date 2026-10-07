package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * Phiếu khám xem bằng mã ngẫu nhiên, không cần đăng nhập (BOOK-05, BOOK-06, quy tắc #8). Ai có link cũng xem được nên
 * CCCD, SĐT bị che và không có id lịch hẹn, id hồ sơ.
 *
 * @param maTraCuu             mã ngắn để đọc / gõ (vd ECL-20261005-4198); không mở được phiếu khám
 * @param linkPhieuKham        link trang phiếu khám trên frontend, cũng là nội dung mã QR
 * @param gioKhamDuKien        giờ bắt đầu lượt khám được xếp
 * @param ngayDat              thời điểm đặt lịch
 * @param nguoiGiamHo          null nếu lượt khám không có người giám hộ
 * @param canNguoiGiamHoDiCung true: hiển thị ghi chú "người giám hộ phải đi cùng" (BOOK-11)
 * @param lyDoHuy              lý do bác sĩ từ chối (BI_TU_CHOI) hoặc lý do hủy; null ở các trạng thái khác
 * @param duocHuyDoi           true nếu lịch còn hủy / đổi được: CHO_XAC_NHAN / DA_XAC_NHAN và chưa quá {@code hanHuyDoi}
 * @param hanHuyDoi            hạn chót hủy / đổi lịch trên hệ thống (giờ khám trừ khoảng tối thiểu)
 * @param canDoiLich           true: ca khám đã bị hủy, lịch hẹn còn hiệu lực và đang chờ đổi sang khung giờ khác (hoặc hủy);
 *                             khi đó hủy / đổi được tới giờ khám cũ và không tính vào số lần đổi lịch
 */
public record PhieuKhamResponse(String maPhieuKham, String maTraCuu, String linkPhieuKham, TrangThaiLichHen trangThai,
        Integer soThuTu, LocalDate ngay, LocalDateTime gioKhamDuKien, LocalDateTime gioBatDauKhung,
        LocalDateTime gioKetThucKhung, BacSiTomTatResponse bacSi, String tenChuyenKhoa,
        PhongKhamTomTatResponse phongKham, String lyDoKham, LocalDateTime ngayDat,
        BenhNhanPhieuKhamResponse benhNhan, NguoiGiamHoPhieuKhamResponse nguoiGiamHo,
        boolean canNguoiGiamHoDiCung, String lyDoHuy, boolean duocHuyDoi, LocalDateTime hanHuyDoi,
        boolean canDoiLich) {
}
