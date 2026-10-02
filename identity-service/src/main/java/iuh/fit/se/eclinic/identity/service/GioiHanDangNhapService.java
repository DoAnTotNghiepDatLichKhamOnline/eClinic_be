package iuh.fit.se.eclinic.identity.service;

/**
 * Đếm số lần nhập sai mật khẩu trên Redis, khoá {@code dang-nhap:sai:<dinhDanh>}. Hai bộ đếm tách nhau:
 * <ul>
 * <li>Đăng nhập: định danh là email (đã chuẩn hoá). Đếm cả email không tồn tại để việc bị khoá không tiết lộ
 * email nào đã đăng ký.</li>
 * <li>Hỏi lại mật khẩu hiện tại khi đang đăng nhập (đổi mật khẩu, đổi email; xem
 * MatKhauService#xacNhanMatKhauHienTai): định danh là {@code doi-mat-khau:<idTaiKhoan>}. Tách riêng để người cầm
 * access token bị lộ không khoá được việc đăng nhập của chủ tài khoản.</li>
 * </ul>
 */
public interface GioiHanDangNhapService {

    /** true nếu định danh đã sai đủ số lần tối đa và còn trong thời gian khoá. */
    boolean dangBiKhoa(String dinhDanh);

    /** Ghi nhận 1 lần sai; trả về số lần sai hiện tại. */
    long ghiNhanThatBai(String dinhDanh);

    /** Xoá bộ đếm (đăng nhập thành công, đặt lại / đổi mật khẩu). */
    void xoa(String dinhDanh);

}
