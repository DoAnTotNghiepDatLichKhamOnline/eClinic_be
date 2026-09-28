package iuh.fit.se.eclinic.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.identity.repository.RefreshTokenRepository;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public String bamToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public Optional<RefreshToken> timPhienConHanTheoToken(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        return refreshTokenRepository.findByTokenHashAndNgayThuHoiIsNull(bamToken(rawToken))
                .filter(token -> token.conHieuLuc(now));
    }

    @Override
    public List<RefreshToken> layPhienDangHoatDong(Long taiKhoanId) {
        return refreshTokenRepository
                .findByTaiKhoanIdAndNgayThuHoiIsNullAndNgayHetHanAfterOrderByNgayTaoDesc(taiKhoanId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void thuHoi(Long refreshTokenId) {
        RefreshToken token = refreshTokenRepository.findById(refreshTokenId)
                .orElseThrow(() -> new LoiKhongTimThay("RefreshToken", refreshTokenId));
        if (token.getNgayThuHoi() == null) {
            token.setNgayThuHoi(LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public int thuHoiTatCaCuaTaiKhoan(Long taiKhoanId) {
        return refreshTokenRepository.revokeAllByTaiKhoanId(taiKhoanId, LocalDateTime.now());
    }

}
