package iuh.fit.se.eclinic.identity.dto.response;

/**
 * Yêu cầu đổi email đang chờ xác nhận của người đang đăng nhập.
 *
 * @param emailMoi địa chỉ đã được gửi liên kết xác nhận
 */
public record YeuCauDoiEmailResponse(String emailMoi) {
}
