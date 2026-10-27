package iuh.fit.se.eclinic.identity.client;

/** Thông tin lấy từ ID token Google đã kiểm tra (chữ ký, iss, aud, hạn, email_verified). */
public record ThongTinGoogle(String sub, String email, String hd, String ten, String anh) {

    /** Google chỉ "đảm bảo" chủ email khi email là @gmail.com hoặc thuộc Google Workspace (có hd). */
    public boolean laGoogleXacNhan() {
        return email.endsWith("@gmail.com") || (hd != null && !hd.isBlank());
    }

}
