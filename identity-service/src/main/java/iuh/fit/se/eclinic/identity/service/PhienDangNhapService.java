package iuh.fit.se.eclinic.identity.service;

import java.util.List;

import iuh.fit.se.eclinic.identity.dto.response.PhienDangNhapResponse;

/** Người đang đăng nhập xem và đăng xuất các thiết bị của chính mình. */
public interface PhienDangNhapService {

    /** Các phiên còn hiệu lực: phiên hiện tại đứng đầu, còn lại theo lần hoạt động gần nhất. */
    List<PhienDangNhapResponse> layDanhSach(Long idTaiKhoan, String maPhienHienTai);

    /** Đăng xuất 1 phiên theo mã. */
    void dangXuat(Long idTaiKhoan, String maPhienHienTai, String maPhien);

    /** Đăng xuất mọi phiên trừ phiên hiện tại; trả về số phiên đã đăng xuất. */
    int dangXuatCacPhienKhac(Long idTaiKhoan, String maPhienHienTai);

}
