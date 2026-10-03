package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.JwtConfig;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;

/**
 * Quên / đặt lại mật khẩu và đổi mật khẩu khi đang đăng nhập, với MySQL + Redis thật. Không gửi email: token lấy từ
 * event đã phát (EmailService thật chạy ở chế độ console và chỉ ghi log).
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@RecordApplicationEvents
class MatKhauServiceTest {

    private static final String MAT_KHAU_CU = "matkhau-cu";
    private static final String MAT_KHAU_MOI = "matkhau-moi";

    @Autowired MatKhauService matKhauService;
    @Autowired DangNhapService dangNhapService;
    @Autowired TokenLienKetService tokenLienKetService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired ApplicationEvents applicationEvents;

    @Test
    void quenMatKhauTaiKhoanDaKichHoatPhatEmail() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);

        matKhauService.quenMatKhau("  " + taiKhoan.getEmail().toUpperCase() + " ");

        List<EmailDatLaiMatKhauEvent> events = cacEmailDatLai();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).email()).isEqualTo(taiKhoan.getEmail());
        assertThat(events.get(0).toString()).doesNotContain(events.get(0).token());
    }

    @Test
    void emailKhongTonTaiChuaKichHoatHoacBiVoHieuHoaKhongGui() {
        matKhauService.quenMatKhau(emailMoi());
        matKhauService.quenMatKhau(taoTaiKhoan(TrangThaiTaiKhoan.CHO_XAC_NHAN).getEmail());
        matKhauService.quenMatKhau(taoTaiKhoan(TrangThaiTaiKhoan.VO_HIEU_HOA).getEmail());

        assertThat(cacEmailDatLai()).isEmpty();
    }

    @Test
    void quenMatKhauLanHaiTrongThoiGianChoKhongGuiThemKhongBaoLoi() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);

        matKhauService.quenMatKhau(taiKhoan.getEmail());
        matKhauService.quenMatKhau(taiKhoan.getEmail());

        assertThat(cacEmailDatLai()).hasSize(1);
    }

    @Test
    void datLaiMatKhauDoiMatKhauDangXuatMoiThietBiVaGuiThongBao() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        String refreshToken = dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU).refreshToken();

        matKhauService.quenMatKhau(taiKhoan.getEmail());
        matKhauService.datLaiMatKhau(tokenCuoiCung(), MAT_KHAU_MOI);

        assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU_MOI).accessToken()).isNotBlank();
        assertMaLoi(() -> dangNhapService.lamMoi(refreshToken), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertThat(applicationEvents.stream(EmailDoiMatKhauEvent.class).toList())
                .containsExactly(new EmailDoiMatKhauEvent(taiKhoan.getEmail(), taiKhoan.getHoTen()));
    }

    @Test
    void tokenChiDungMotLanVaTokenRacBao410() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        matKhauService.quenMatKhau(taiKhoan.getEmail());
        String token = tokenCuoiCung();
        matKhauService.datLaiMatKhau(token, MAT_KHAU_MOI);

        assertMaLoi(() -> matKhauService.datLaiMatKhau(token, "matkhau-khac"), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertMaLoi(() -> matKhauService.datLaiMatKhau("token-rac", MAT_KHAU_MOI), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU_MOI).accessToken()).isNotBlank();
    }

    @Test
    void tokenKichHoatTaiKhoanKhongDungDeDatLaiMatKhau() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        String tokenKichHoat = tokenLienKetService.tao(taiKhoan.getId(), MucDichLienKet.XAC_THUC_EMAIL,
                Duration.ofMinutes(5));

        assertMaLoi(() -> matKhauService.datLaiMatKhau(tokenKichHoat, MAT_KHAU_MOI), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU).accessToken()).isNotBlank();
    }

    @Test
    void dangBiKhoaDangNhapThiDatLaiXongDangNhapDuoc() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        for (int i = 0; i < 5; i++) {
            assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), "sai-mat-khau"), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        }
        assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU), MaLoi.DANG_NHAP_SAI_QUA_NHIEU);

        matKhauService.quenMatKhau(taiKhoan.getEmail());
        matKhauService.datLaiMatKhau(tokenCuoiCung(), MAT_KHAU_MOI);

        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU_MOI).accessToken()).isNotBlank();
    }

    @Test
    void taiKhoanBiVoHieuHoaSauKhiGuiLienKetBao403() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        matKhauService.quenMatKhau(taiKhoan.getEmail());
        TaiKhoan moiNhat = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        moiNhat.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoanRepository.save(moiNhat);

        assertMaLoi(() -> matKhauService.datLaiMatKhau(tokenCuoiCung(), MAT_KHAU_MOI),
                MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertThat(passwordEncoder.matches(MAT_KHAU_CU,
                taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getMatKhauHash())).isTrue();
    }

    @Test
    void taiKhoanChiCoGoogleDatDuocMatKhauQuaQuenMatKhau() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        String googleId = "google-" + UUID.randomUUID();
        taiKhoan.setMatKhauHash(null);
        taiKhoan.setGoogleId(googleId);
        taiKhoanRepository.save(taiKhoan);
        assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU), MaLoi.SAI_THONG_TIN_DANG_NHAP);

        matKhauService.quenMatKhau(taiKhoan.getEmail());
        matKhauService.datLaiMatKhau(tokenCuoiCung(), MAT_KHAU_MOI);

        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU_MOI).accessToken()).isNotBlank();
        // Vẫn giữ liên kết Google
        assertThat(taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getGoogleId()).isEqualTo(googleId);
    }

    @Test
    void doiMatKhauGiuPhienHienTaiDangXuatPhienKhacVaGuiThongBao() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse mayA = dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU);
        DangNhapResponse mayB = dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU);

        matKhauService.doiMatKhau(taiKhoan.getId(), maPhien(mayA), new DoiMatKhauRequest(MAT_KHAU_CU, MAT_KHAU_MOI));

        assertThat(dangNhapService.lamMoi(mayA.refreshToken()).accessToken()).isNotBlank();
        assertMaLoi(() -> dangNhapService.lamMoi(mayB.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU_MOI).accessToken()).isNotBlank();
        assertThat(applicationEvents.stream(EmailDoiMatKhauEvent.class).toList())
                .containsExactly(new EmailDoiMatKhauEvent(taiKhoan.getEmail(), taiKhoan.getHoTen()));
    }

    @Test
    void doiMatKhauKhongCoPhienHienTaiThiDangXuatTatCa() {
        // Access token không có claim "phien" (maPhienHienTai = null): không biết giữ phiên nào nên thu hồi hết
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse mayA = dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU);
        DangNhapResponse mayB = dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU);

        matKhauService.doiMatKhau(taiKhoan.getId(), null, new DoiMatKhauRequest(MAT_KHAU_CU, MAT_KHAU_MOI));

        assertMaLoi(() -> dangNhapService.lamMoi(mayA.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertMaLoi(() -> dangNhapService.lamMoi(mayB.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU_MOI).accessToken()).isNotBlank();
    }

    @Test
    void saiMatKhauCu5LanThiKhoaDoiMatKhauNhungVanDangNhapDuoc() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        for (int i = 0; i < 5; i++) {
            assertMaLoi(() -> doiMatKhau(taiKhoan, "sai-mat-khau", MAT_KHAU_MOI), MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        }

        // Đã khoá: mật khẩu hiện tại đúng cũng bị từ chối, mật khẩu không đổi
        assertMaLoi(() -> doiMatKhau(taiKhoan, MAT_KHAU_CU, MAT_KHAU_MOI), MaLoi.SAI_MAT_KHAU_QUA_NHIEU);
        // Bộ đếm tách khỏi đăng nhập: chủ tài khoản vẫn đăng nhập được bằng mật khẩu cũ
        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU).accessToken()).isNotBlank();
        assertThat(applicationEvents.stream(EmailDoiMatKhauEvent.class).toList()).isEmpty();
    }

    @Test
    void doiMatKhauThanhCongXoaBoDemSai() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        for (int i = 0; i < 4; i++) {
            assertMaLoi(() -> doiMatKhau(taiKhoan, "sai-mat-khau", MAT_KHAU_MOI), MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        }
        doiMatKhau(taiKhoan, MAT_KHAU_CU, MAT_KHAU_MOI);

        // Nếu bộ đếm còn 4 thì lần sai này là lần thứ 5 và lần đổi sau đó bị 429
        assertMaLoi(() -> doiMatKhau(taiKhoan, "sai-mat-khau", "matkhau-khac"), MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        doiMatKhau(taiKhoan, MAT_KHAU_MOI, "matkhau-khac");
        assertThat(dangNhap(taiKhoan.getEmail(), "matkhau-khac").accessToken()).isNotBlank();
    }

    @Test
    void matKhauCuDungThemKyTuSauByte72Bao400() {
        // BCrypt chỉ đọc 72 byte đầu: không chặn thì "mật khẩu + x" cũng được coi là đúng
        String matKhau72Byte = "a".repeat(72);
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, matKhau72Byte);

        assertMaLoi(() -> doiMatKhau(taiKhoan, matKhau72Byte + "x", MAT_KHAU_MOI), MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        assertThat(dangNhap(taiKhoan.getEmail(), matKhau72Byte).accessToken()).isNotBlank();
    }

    @Test
    void matKhauMoiTrungMatKhauCuBao400() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        String refreshToken = dangNhap(taiKhoan.getEmail(), MAT_KHAU_CU).refreshToken();

        assertMaLoi(() -> doiMatKhau(taiKhoan, MAT_KHAU_CU, MAT_KHAU_CU), MaLoi.MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU);
        // Không đổi gì: phiên vẫn còn
        assertThat(dangNhapService.lamMoi(refreshToken).accessToken()).isNotBlank();
    }

    @Test
    void doiMatKhauTaiKhoanChiCoGoogleBao409() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        taiKhoan.setMatKhauHash(null);
        taiKhoan.setGoogleId("google-" + UUID.randomUUID());
        taiKhoanRepository.save(taiKhoan);

        assertMaLoi(() -> doiMatKhau(taiKhoan, MAT_KHAU_CU, MAT_KHAU_MOI), MaLoi.TAI_KHOAN_CHUA_CO_MAT_KHAU);
        assertThat(taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getMatKhauHash()).isNull();
    }

    @Test
    void doiMatKhauTaiKhoanBiVoHieuHoaBao403TaiKhoanKhongConBao401() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.VO_HIEU_HOA);

        assertMaLoi(() -> doiMatKhau(taiKhoan, MAT_KHAU_CU, MAT_KHAU_MOI), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertThat(passwordEncoder.matches(MAT_KHAU_CU,
                taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getMatKhauHash())).isTrue();
        assertMaLoi(() -> matKhauService.doiMatKhau(Long.MAX_VALUE, null,
                new DoiMatKhauRequest(MAT_KHAU_CU, MAT_KHAU_MOI)), MaLoi.CHUA_DANG_NHAP);
    }

    private void doiMatKhau(TaiKhoan taiKhoan, String matKhauCu, String matKhauMoi) {
        matKhauService.doiMatKhau(taiKhoan.getId(), null, new DoiMatKhauRequest(matKhauCu, matKhauMoi));
    }

    /** Mã phiên trong access token (claim "phien"). */
    private String maPhien(DangNhapResponse ketQua) {
        return jwtDecoder.decode(ketQua.accessToken()).getClaimAsString(JwtConfig.CLAIM_PHIEN);
    }

    private TaiKhoan taoTaiKhoan(TrangThaiTaiKhoan trangThai) {
        return taoTaiKhoan(trangThai, MAT_KHAU_CU);
    }

    private TaiKhoan taoTaiKhoan(TrangThaiTaiKhoan trangThai, String matKhau) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Bệnh nhân test");
        taiKhoan.setEmail(emailMoi());
        taiKhoan.setMatKhauHash(passwordEncoder.encode(matKhau));
        taiKhoan.setVaiTro(VaiTro.BENH_NHAN);
        taiKhoan.setTrangThai(trangThai);
        return taiKhoanRepository.save(taiKhoan);
    }

    private DangNhapResponse dangNhap(String email, String matKhau) {
        return dangNhapService.dangNhap(new DangNhapRequest(email, matKhau), "JUnit");
    }

    private List<EmailDatLaiMatKhauEvent> cacEmailDatLai() {
        return applicationEvents.stream(EmailDatLaiMatKhauEvent.class).toList();
    }

    private String tokenCuoiCung() {
        List<EmailDatLaiMatKhauEvent> events = cacEmailDatLai();
        assertThat(events).isNotEmpty();
        return events.get(events.size() - 1).token();
    }

    private static void assertMaLoi(Runnable hanhDong, MaLoi maLoi) {
        assertThatThrownBy(hanhDong::run)
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

    private static String emailMoi() {
        return "mk-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

}
