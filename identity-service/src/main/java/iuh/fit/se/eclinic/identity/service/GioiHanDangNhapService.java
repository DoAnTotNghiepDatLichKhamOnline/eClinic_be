package iuh.fit.se.eclinic.identity.service;

/**
 * Đếm số lần đăng nhập sai theo email (đã chuẩn hoá) trên Redis, khoá {@code dang-nhap:sai:<email>}.
 * Đếm cả email không tồn tại để việc bị khoá không tiết lộ email nào đã đăng ký.
 */
public interface GioiHanDangNhapService {

    /** true nếu email đã sai đủ số lần tối đa và còn trong thời gian khoá. */
    boolean dangBiKhoa(String email);

    /** Ghi nhận 1 lần sai; trả về số lần sai hiện tại. */
    long ghiNhanThatBai(String email);

    /** Xoá bộ đếm (đăng nhập thành công, đặt lại mật khẩu). */
    void xoa(String email);

}
