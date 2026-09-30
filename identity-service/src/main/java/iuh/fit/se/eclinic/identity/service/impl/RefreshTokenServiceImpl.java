package iuh.fit.se.eclinic.identity.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.identity.repository.RefreshTokenRepository;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.util.TokenNgauNhien;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

    /** Độ dài cột thong_tin_thiet_bi (V1). */
    private static final int DO_DAI_THIET_BI_TOI_DA = 255;

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public String bamToken(String rawToken) {
        return TokenNgauNhien.bam(rawToken);
    }

    @Override
    @Transactional
    public String tao(TaiKhoan taiKhoan, String thongTinThietBi, Duration thoiHan) {
        String token = TokenNgauNhien.tao();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTaiKhoan(taiKhoan);
        refreshToken.setTokenHash(bamToken(token));
        refreshToken.setThongTinThietBi(catNgan(thongTinThietBi));
        refreshToken.setNgayHetHan(LocalDateTime.now().plus(thoiHan));
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Override
    public Optional<RefreshToken> timTheoToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(bamToken(rawToken));
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
    public boolean thuHoiNeuConHieuLuc(Long refreshTokenId) {
        return refreshTokenRepository.revokeIfActive(refreshTokenId, LocalDateTime.now()) == 1;
    }

    @Override
    @Transactional
    public int thuHoiTatCaCuaTaiKhoan(Long taiKhoanId) {
        return refreshTokenRepository.revokeAllByTaiKhoanId(taiKhoanId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int xoaPhienHetHan() {
        return refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now());
    }

    private static String catNgan(String thongTinThietBi) {
        if (thongTinThietBi == null || thongTinThietBi.length() <= DO_DAI_THIET_BI_TOI_DA) {
            return thongTinThietBi;
        }
        return thongTinThietBi.substring(0, DO_DAI_THIET_BI_TOI_DA);
    }

}
