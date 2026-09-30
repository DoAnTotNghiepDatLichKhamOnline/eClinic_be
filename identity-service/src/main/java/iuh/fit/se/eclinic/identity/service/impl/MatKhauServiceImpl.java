package iuh.fit.se.eclinic.identity.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.LienKetProperties;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.GioiHanDangNhapService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatKhauServiceImpl implements MatKhauService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final TokenLienKetService tokenLienKetService;
    private final RefreshTokenService refreshTokenService;
    private final GioiHanDangNhapService gioiHanDangNhapService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final LienKetProperties lienKetProperties;

    @Override
    @Transactional
    public void quenMatKhau(String email) {
        // Chưa hết thời gian chờ thì im lặng không gửi (không báo 429: 429 sẽ lộ email nào có tài khoản)
        taiKhoanRepository.findByEmail(ChuanHoa.email(email))
                .filter(tk -> tk.getTrangThai() == TrangThaiTaiKhoan.DA_KICH_HOAT)
                .filter(tk -> tokenLienKetService.giuCho(tk.getId(), MucDichLienKet.DAT_LAI_MAT_KHAU,
                        lienKetProperties.thoiGianChoGuiLai()))
                .ifPresent(tk -> {
                    // Sau khi giữ được khoá chờ (quy ước của TokenLienKetService); liên kết cũ hết hiệu lực
                    String token = tokenLienKetService.tao(tk.getId(), MucDichLienKet.DAT_LAI_MAT_KHAU,
                            lienKetProperties.thoiHanDatLaiMatKhau());
                    eventPublisher.publishEvent(new EmailDatLaiMatKhauEvent(tk.getEmail(), tk.getHoTen(), token));
                    log.info("Gửi liên kết đặt lại mật khẩu id={}", tk.getId());
                });
    }

    @Override
    @Transactional
    public void datLaiMatKhau(String token, String matKhauMoi) {
        // Mọi lỗi đều ném trước khi ghi DB, nên transaction không có gì phải giữ lại khi lỗi
        Long idTaiKhoan = tokenLienKetService.suDung(token, MucDichLienKet.DAT_LAI_MAT_KHAU);
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE));
        switch (taiKhoan.getTrangThai()) {
            case VO_HIEU_HOA -> throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
            // Không gửi liên kết cho tài khoản chưa kích hoạt, nên không thể xảy ra
            case CHO_XAC_NHAN -> throw new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE);
            case DA_KICH_HOAT -> {
                // Được đặt lại
            }
        }

        taiKhoan.setMatKhauHash(passwordEncoder.encode(matKhauMoi));
        // revokeAllByTaiKhoanId flush trước rồi mới clear persistence context, nên mật khẩu mới không bị mất
        int soPhien = refreshTokenService.thuHoiTatCaCuaTaiKhoan(idTaiKhoan);
        gioiHanDangNhapService.xoa(taiKhoan.getEmail());
        eventPublisher.publishEvent(new EmailDoiMatKhauEvent(taiKhoan.getEmail(), taiKhoan.getHoTen()));
        log.info("Đặt lại mật khẩu id={}, thu hồi {} phiên", idTaiKhoan, soPhien);
    }

}
