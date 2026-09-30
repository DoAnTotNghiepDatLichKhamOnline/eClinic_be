package iuh.fit.se.eclinic.identity.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/**
 * Quản lý phiên đăng nhập. Token gốc chỉ nằm ở client; DB chỉ giữ SHA-256 của token.
 */
public interface RefreshTokenService {

    /** SHA-256 hex của refresh token gốc. */
    String bamToken(String rawToken);

    /** Tạo phiên mới, lưu hash; trả về token gốc để gửi cho client (chỉ lần này). */
    String tao(TaiKhoan taiKhoan, String thongTinThietBi, Duration thoiHan);

    /** Phiên tương ứng với token gốc, bất kể đã thu hồi hay hết hạn. */
    Optional<RefreshToken> timTheoToken(String rawToken);

    /** Phiên còn hiệu lực (chưa thu hồi, chưa hết hạn) tương ứng với token gốc. */
    Optional<RefreshToken> timPhienConHanTheoToken(String rawToken);

    /** Danh sách thiết bị đang đăng nhập của tài khoản. */
    List<RefreshToken> layPhienDangHoatDong(Long taiKhoanId);

    void thuHoi(Long refreshTokenId);

    /** Thu hồi nguyên tử: true nếu lời gọi này là lời gọi thu hồi phiên (phiên còn hiệu lực trước đó). */
    boolean thuHoiNeuConHieuLuc(Long refreshTokenId);

    /** Đăng xuất khỏi tất cả thiết bị (AUTH-03 sau khi đặt lại mật khẩu, ADM-03 khi vô hiệu hóa tài khoản). */
    int thuHoiTatCaCuaTaiKhoan(Long taiKhoanId);

    /** Xoá các phiên đã hết hạn; trả về số dòng đã xoá. */
    int xoaPhienHetHan();

}
