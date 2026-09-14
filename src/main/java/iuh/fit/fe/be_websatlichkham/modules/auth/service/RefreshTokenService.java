package iuh.fit.fe.be_websatlichkham.modules.auth.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.modules.auth.entity.RefreshToken;

/**
 * Quản lý phiên đăng nhập. Token gốc chỉ nằm ở client; DB chỉ giữ SHA-256 của token.
 */
public interface RefreshTokenService {

    /** SHA-256 hex của refresh token gốc. */
    String hashToken(String rawToken);

    /** Phiên còn hiệu lực (chưa thu hồi, chưa hết hạn) tương ứng với token gốc. */
    Optional<RefreshToken> findActiveByRawToken(String rawToken);

    /** Danh sách thiết bị đang đăng nhập của user. */
    List<RefreshToken> findActiveSessions(Long userId);

    void revoke(Long refreshTokenId);

    /** Đăng xuất khỏi tất cả thiết bị (vd: sau khi đổi mật khẩu). */
    int revokeAllForUser(Long userId);

}
