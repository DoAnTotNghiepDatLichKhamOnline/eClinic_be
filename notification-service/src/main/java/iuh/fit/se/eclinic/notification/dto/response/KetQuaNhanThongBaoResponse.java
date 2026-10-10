package iuh.fit.se.eclinic.notification.dto.response;

/**
 * Kết quả nhận 1 lô thông báo từ service khác.
 *
 * @param daTao số thông báo vừa tạo
 * @param boQua số dòng không tạo: sự kiện đã nhận trước đó (gửi lại), hoặc tài khoản nhận không còn tồn tại
 */
public record KetQuaNhanThongBaoResponse(int daTao, int boQua) {
}
