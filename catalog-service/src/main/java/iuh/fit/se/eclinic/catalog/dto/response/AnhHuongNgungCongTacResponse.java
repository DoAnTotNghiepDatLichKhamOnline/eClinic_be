package iuh.fit.se.eclinic.catalog.dto.response;

/**
 * Những gì sẽ bị ảnh hưởng nếu cho bác sĩ ngừng công tác ngay lúc này, để quản trị viên xem trước khi xác nhận.
 *
 * @param soCaSapToi          số ca còn hoạt động chưa bắt đầu sẽ bị hủy
 * @param soLichHenBiAnhHuong số lịch hẹn còn hiệu lực trên các ca đó, sẽ được giữ và đánh dấu cần đổi lịch
 */
public record AnhHuongNgungCongTacResponse(long soCaSapToi, long soLichHenBiAnhHuong) {
}
