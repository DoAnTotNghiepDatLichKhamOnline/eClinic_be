package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;

/** Đăng nhập bằng Google (chỉ bệnh nhân): kiểm tra ID token, tìm / liên kết / tạo tài khoản, cấp phiên. */
public interface DangNhapGoogleService {

    /** Tìm tài khoản theo google_id, rồi theo email. */
    DangNhapResponse dangNhap(String idToken, String thongTinThietBi);

}
