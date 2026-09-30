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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;

/**
 * Quên / đặt lại mật khẩu với MySQL + Redis thật. Không gửi email: token lấy từ event đã phát
 * (EmailService thật chạy ở chế độ console và chỉ ghi log).
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

    private TaiKhoan taoTaiKhoan(TrangThaiTaiKhoan trangThai) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Bệnh nhân test");
        taiKhoan.setEmail(emailMoi());
        taiKhoan.setMatKhauHash(passwordEncoder.encode(MAT_KHAU_CU));
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
