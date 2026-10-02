package iuh.fit.se.eclinic.identity.service.impl;

import java.nio.charset.StandardCharsets;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.DangNhapProperties;
import iuh.fit.se.eclinic.identity.config.LienKetProperties;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.GioiHanDangNhapService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.service.TaiKhoanService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatKhauServiceImpl implements MatKhauService {

    private static final int SO_BYTE_MAT_KHAU_TOI_DA = 72;
    /**
     * Định danh bộ đếm sai mật khẩu hiện tại (xem GioiHanDangNhapService): theo tài khoản, tách khỏi bộ đếm đăng nhập.
     * Đổi mật khẩu và đổi email dùng chung bộ đếm này.
     */
    private static final String KHOA_DEM_MAT_KHAU_HIEN_TAI = "doi-mat-khau:";

    private final TaiKhoanRepository taiKhoanRepository;
    private final TaiKhoanService taiKhoanService;
    private final DangNhapProperties dangNhapProperties;
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
        // Yêu cầu đổi email đang chờ (nếu có) được tạo bằng mật khẩu cũ: huỷ
        tokenLienKetService.huy(idTaiKhoan, MucDichLienKet.DOI_EMAIL);
        eventPublisher.publishEvent(new EmailDoiMatKhauEvent(taiKhoan.getEmail(), taiKhoan.getHoTen()));
        log.info("Đặt lại mật khẩu id={}, thu hồi {} phiên", idTaiKhoan, soPhien);
    }

    @Override
    @Transactional
    public void doiMatKhau(Long idTaiKhoan, String maPhienHienTai, DoiMatKhauRequest request) {
        // Mọi lỗi đều ném trước khi ghi DB; bộ đếm sai nằm ở Redis nên không mất khi transaction rollback
        TaiKhoan taiKhoan = taiKhoanService.layDangHoatDong(idTaiKhoan);
        xacNhanMatKhauHienTai(taiKhoan, request.matKhauCu());
        // Mật khẩu hiện tại đã đúng nên so sánh chuỗi là đủ
        if (request.matKhauMoi().equals(request.matKhauCu())) {
            throw new LoiNghiepVu(MaLoi.MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU);
        }

        taiKhoan.setMatKhauHash(passwordEncoder.encode(request.matKhauMoi()));
        // UPDATE thu hồi flush trước rồi mới clear persistence context, nên mật khẩu mới không bị mất
        int soPhien = refreshTokenService.thuHoiCacPhienKhac(idTaiKhoan, maPhienHienTai);
        gioiHanDangNhapService.xoa(taiKhoan.getEmail());
        // Người vừa đổi mật khẩu vì nhận thông báo "có yêu cầu đổi email": liên kết đổi email phải chết theo
        tokenLienKetService.huy(idTaiKhoan, MucDichLienKet.DOI_EMAIL);
        eventPublisher.publishEvent(new EmailDoiMatKhauEvent(taiKhoan.getEmail(), taiKhoan.getHoTen()));
        log.info("Đổi mật khẩu id={}, thu hồi {} phiên khác", idTaiKhoan, soPhien);
    }

    @Override
    public void xacNhanMatKhauHienTai(TaiKhoan taiKhoan, String matKhau) {
        if (taiKhoan.getMatKhauHash() == null) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_CHUA_CO_MAT_KHAU);
        }
        Long idTaiKhoan = taiKhoan.getId();
        String khoaDem = KHOA_DEM_MAT_KHAU_HIEN_TAI + idTaiKhoan;
        // Đang bị khoá thì từ chối ngay, không chạy BCrypt
        if (gioiHanDangNhapService.dangBiKhoa(khoaDem)) {
            throw new LoiNghiepVu(MaLoi.SAI_MAT_KHAU_QUA_NHIEU);
        }
        if (!khopMatKhau(matKhau, taiKhoan.getMatKhauHash())) {
            long soLanSai = gioiHanDangNhapService.ghiNhanThatBai(khoaDem);
            log.info("Sai mật khẩu hiện tại id={} (lần sai thứ {})", idTaiKhoan, soLanSai);
            if (soLanSai >= dangNhapProperties.soLanSaiToiDa()) {
                log.warn("Khoá đổi mật khẩu id={} sau {} lần sai mật khẩu hiện tại (khoá cả đổi email)", idTaiKhoan,
                        soLanSai);
            }
            throw new LoiNghiepVu(MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        }
        gioiHanDangNhapService.xoa(khoaDem);
    }

    /** Cùng quy tắc 72 byte với đăng nhập (xem DangNhapServiceImpl): BCrypt bỏ qua phần sau byte 72. */
    private boolean khopMatKhau(String matKhau, String hash) {
        return matKhau.getBytes(StandardCharsets.UTF_8).length <= SO_BYTE_MAT_KHAU_TOI_DA
                && passwordEncoder.matches(matKhau, hash);
    }

}
