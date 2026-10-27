package iuh.fit.se.eclinic.identity.service;

/** Đếm số lần nhập sai mật khẩu trên Redis, khoá {@code dang-nhap:sai:<dinhDanh>}. */
public interface GioiHanDangNhapService {

    /** true nếu định danh đã sai đủ số lần tối đa và còn trong thời gian khoá. */
    boolean dangBiKhoa(String dinhDanh);

    /** Ghi nhận 1 lần sai; trả về số lần sai hiện tại. */
    long ghiNhanThatBai(String dinhDanh);

    /** Xoá bộ đếm (đăng nhập thành công, đặt lại / đổi mật khẩu). */
    void xoa(String dinhDanh);

}
