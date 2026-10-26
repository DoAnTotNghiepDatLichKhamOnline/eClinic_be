package iuh.fit.se.eclinic.identity.client;

/**
 * Thông tin lấy từ ID token Google đã kiểm tra (chữ ký, iss, aud, hạn, email_verified).
 *
 * @param sub   định danh bất biến của tài khoản Google (dùng làm khoá liên kết, không dùng email)
 * @param email email đã chuẩn hoá (trim + chữ thường)
 * @param hd    tên miền Google Workspace, null với tài khoản cá nhân
 * @param ten   họ tên hiển thị trên Google, có thể null
 * @param anh   URL ảnh đại diện, có thể null
 */
public record ThongTinGoogle(String sub, String email, String hd, String ten, String anh) {

    /**
     * Google chỉ "đảm bảo" chủ email khi email là @gmail.com hoặc thuộc Google Workspace (có hd).
     * Email khác (vd @yahoo.com) chỉ được xác minh lúc tạo tài khoản Google, nay có thể đã đổi chủ.
     */
    public boolean laGoogleXacNhan() {
        return email.endsWith("@gmail.com") || (hd != null && !hd.isBlank());
    }

}
