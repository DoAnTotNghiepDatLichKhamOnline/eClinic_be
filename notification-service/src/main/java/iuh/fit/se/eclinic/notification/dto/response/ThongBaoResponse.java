package iuh.fit.se.eclinic.notification.dto.response;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.LoaiThongBao;

/**
 * 1 thông báo của tài khoản đang đăng nhập.
 *
 * @param noiDung     câu thông báo đã soạn sẵn (tiếng Việt), hiển thị nguyên văn
 * @param idLichHen   lịch hẹn liên quan; null nếu thông báo không gắn với lịch hẹn
 * @param maPhieuKham mã phiếu khám của lịch hẹn đó, CHỈ có khi người đọc là bệnh nhân (để mở chi tiết lịch hẹn / phiếu
 *                    khám); bác sĩ và quản trị viên dùng {@code idLichHen}
 * @param idYeuCau    yêu cầu đổi ca / xin nghỉ liên quan (thông báo cho quản trị viên / bác sĩ); null nếu không có
 */
public record ThongBaoResponse(Long id, LoaiThongBao loai, String noiDung, boolean daDoc, LocalDateTime ngayTao,
        Long idLichHen, String maPhieuKham, Long idYeuCau) {
}
