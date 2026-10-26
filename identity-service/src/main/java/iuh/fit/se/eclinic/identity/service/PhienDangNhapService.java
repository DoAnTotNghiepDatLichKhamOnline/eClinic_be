package iuh.fit.se.eclinic.identity.service;

import java.util.List;

import iuh.fit.se.eclinic.identity.dto.response.PhienDangNhapResponse;

/**
 * Người đang đăng nhập xem và đăng xuất các thiết bị của chính mình. "Phiên hiện tại" là phiên có mã trùng claim
 * "phien" của access token; token không có claim ({@code maPhienHienTai} null) thì không phiên nào là hiện tại.
 * Mọi method ném lỗi của {@link TaiKhoanService#layDangHoatDong} khi tài khoản không còn / bị vô hiệu hoá.
 */
public interface PhienDangNhapService {

    /** Các phiên còn hiệu lực: phiên hiện tại đứng đầu, còn lại theo lần hoạt động gần nhất. */
    List<PhienDangNhapResponse> layDanhSach(Long idTaiKhoan, String maPhienHienTai);

    /**
     * Đăng xuất 1 phiên theo mã. Ném KHONG_TIM_THAY nếu mã sai, là phiên của tài khoản khác hoặc phiên đã đăng xuất /
     * hết hạn; DU_LIEU_KHONG_HOP_LE nếu là phiên hiện tại (phải dùng đăng xuất để cookie cũng được xoá).
     */
    void dangXuat(Long idTaiKhoan, String maPhienHienTai, String maPhien);

    /** Đăng xuất mọi phiên trừ phiên hiện tại; trả về số phiên đã đăng xuất. */
    int dangXuatCacPhienKhac(Long idTaiKhoan, String maPhienHienTai);

}
