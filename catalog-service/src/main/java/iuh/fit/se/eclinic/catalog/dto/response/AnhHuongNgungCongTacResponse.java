package iuh.fit.se.eclinic.catalog.dto.response;

/** Những gì sẽ bị ảnh hưởng nếu cho bác sĩ ngừng công tác ngay lúc này, để quản trị viên xem trước khi xác nhận. */
public record AnhHuongNgungCongTacResponse(long soCaSapToi, long soLichHenBiAnhHuong) {
}
