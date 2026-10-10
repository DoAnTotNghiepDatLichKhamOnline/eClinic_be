package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * 1 dòng trong "lịch hẹn của tôi" (BOOK-07). Không có id lịch hẹn: xem chi tiết / QR bằng phiếu khám theo
 * {@code maPhieuKham} (quy tắc #8).
 *
 * @param maTraCuu         mã ngắn để đọc / gõ (vd ECL-20261005-4198)
 * @param hoTenBenhNhan    {@code laBanThan}: họ tên trong hồ sơ của chủ tài khoản; còn lại: họ tên người đặt đã nhập
 * @param laBanThan        true nếu người khám là chủ tài khoản (lịch dùng hồ sơ bệnh nhân của tài khoản), bất kể ai
 *                         đặt; false là lịch của người khác (đặt cho người thân, hoặc chủ tài khoản là người giám hộ)
 * @param hoTenNguoiGiamHo null nếu lượt khám không có người giám hộ
 * @param ngayDat          thời điểm đặt lịch
 * @param nguoiDat         ai đã đặt lịch này, nhìn từ tài khoản đang xem
 * @param thongTinKhacHoSo chỉ có nghĩa khi {@code laBanThan}: người đặt đã nhập họ tên / ngày sinh khác hồ sơ của chủ
 *                         tài khoản và phòng khám chưa đối chiếu (có thể là lịch không phải của mình). Luôn false với
 *                         lịch của người khác
 * @param lyDoHuy          lý do bác sĩ từ chối (BI_TU_CHOI) hoặc lý do hủy; null ở các trạng thái khác
 * @param duocHuyDoi       true nếu tài khoản đang xem còn hủy / đổi được lịch này: lịch CHO_XAC_NHAN / DA_XAC_NHAN, chưa
 *                         quá {@code hanHuyDoi}, và tài khoản là người đặt hoặc người khám ({@code laBanThan})
 * @param hanHuyDoi        hạn chót hủy / đổi lịch trên hệ thống (giờ khám trừ khoảng tối thiểu)
 * @param canDoiLich           true: ca khám đã bị hủy, lịch hẹn còn hiệu lực và đang chờ đổi sang khung giờ khác (hoặc hủy);
 *                             khi đó hủy / đổi được tới giờ khám cũ và không tính vào số lần đổi lịch
 */
public record LichHenCuaToiResponse(String maPhieuKham, String maTraCuu, String linkPhieuKham, TrangThaiLichHen trangThai,
        Integer soThuTu, LocalDate ngay, LocalDateTime gioKhamDuKien, LocalDateTime gioBatDauKhung,
        LocalDateTime gioKetThucKhung, BacSiTomTatResponse bacSi, String tenChuyenKhoa,
        PhongKhamTomTatResponse phongKham, String hoTenBenhNhan, boolean laBanThan, String hoTenNguoiGiamHo,
        String lyDoKham, LocalDateTime ngayDat, NguoiDatLich nguoiDat, boolean thongTinKhacHoSo,
        String lyDoHuy, boolean duocHuyDoi, LocalDateTime hanHuyDoi, boolean canDoiLich) {
}
