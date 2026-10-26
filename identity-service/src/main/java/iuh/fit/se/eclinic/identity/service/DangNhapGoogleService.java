package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;

/**
 * Đăng nhập bằng Google (chỉ tài khoản bệnh nhân): kiểm tra ID token, tìm / liên kết / tạo tài khoản, cấp phiên như
 * đăng nhập mật khẩu.
 */
public interface DangNhapGoogleService {

    /**
     * Tìm tài khoản theo google_id, rồi theo email. Email đã có tài khoản chỉ được liên kết khi Google đảm bảo chủ
     * email (@gmail.com hoặc Google Workspace); chưa có thì tạo tài khoản bệnh nhân đã kích hoạt, không mật khẩu.
     *
     * @param idToken         ID token Google Identity Services cấp cho frontend
     * @param thongTinThietBi User-Agent của client, lưu cùng phiên (có thể null)
     */
    DangNhapResponse dangNhap(String idToken, String thongTinThietBi);

}
