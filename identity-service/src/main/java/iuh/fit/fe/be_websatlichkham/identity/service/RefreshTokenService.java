package iuh.fit.fe.be_websatlichkham.identity.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.common.entity.identity.RefreshToken;

/**
 * Quản lý phiên đăng nhập. Token gốc chỉ nằm ở client; DB chỉ giữ SHA-256 của token.
 */
public interface RefreshTokenService {

    /** SHA-256 hex của refresh token gốc. */
    String hashToken(String rawToken);

    /** Phiên còn hiệu lực (chưa thu hồi, chưa hết hạn) tương ứng với token gốc. */
    Optional<RefreshToken> findActiveByRawToken(String rawToken);

    /** Danh sách thiết bị đang đăng nhập của tài khoản. */
    List<RefreshToken> findActiveSessions(Long taiKhoanId);

    void revoke(Long refreshTokenId);

    /** Đăng xuất khỏi tất cả thiết bị (AUTH-03 sau khi đặt lại mật khẩu, ADM-03 khi vô hiệu hóa tài khoản). */
    int revokeAllForTaiKhoan(Long taiKhoanId);

}
