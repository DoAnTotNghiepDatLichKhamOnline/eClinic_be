package iuh.fit.fe.be_websatlichkham.modules.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.auth.entity.RefreshToken;
import iuh.fit.fe.be_websatlichkham.modules.auth.repository.RefreshTokenRepository;
import iuh.fit.fe.be_websatlichkham.modules.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public Optional<RefreshToken> findActiveByRawToken(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        return refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashToken(rawToken))
                .filter(token -> token.isActive(now));
    }

    @Override
    public List<RefreshToken> findActiveSessions(Long userId) {
        return refreshTokenRepository
                .findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void revoke(Long refreshTokenId) {
        RefreshToken token = refreshTokenRepository.findById(refreshTokenId)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", refreshTokenId));
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public int revokeAllForUser(Long userId) {
        return refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
    }

}
