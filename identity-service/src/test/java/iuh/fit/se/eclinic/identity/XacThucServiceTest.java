package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanResponse;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.XacThucService;

/** Đăng ký / kích hoạt với MySQL + Redis thật. */
class XacThucServiceTest extends KiemThuTichHop {

    @Autowired XacThucService xacThucService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ApplicationEvents applicationEvents;

    @Test
    void dangKyTaoTaiKhoanChoXacThucVaPhatEmail() {
        String hauTo = hauTo();
        TaiKhoanResponse ketQua = xacThucService.dangKy(
                new DangKyRequest("  Nguyễn Văn A ", "  BN-" + hauTo + "@Example.COM ", "123456", soDienThoai(), null));

        TaiKhoan taiKhoan = taiKhoanRepository.findById(ketQua.id()).orElseThrow();
        assertThat(taiKhoan.getEmail()).isEqualTo("bn-" + hauTo + "@example.com");
        assertThat(taiKhoan.getHoTen()).isEqualTo("Nguyễn Văn A");
        assertThat(taiKhoan.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.CHO_XAC_NHAN);
        assertThat(taiKhoan.getVaiTro()).isEqualTo(VaiTro.BENH_NHAN);
        assertThat(passwordEncoder.matches("123456", taiKhoan.getMatKhauHash())).isTrue();

        List<EmailXacThucEvent> events = cacEmailDaPhat();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).email()).isEqualTo(taiKhoan.getEmail());
    }

    @Test
    void xacThucEmailKichHoatVaLienKetChiDungMotLan() {
        TaiKhoanResponse ketQua = dangKy(emailMoi(), soDienThoai(), "123456");
        String token = tokenCuoiCung();

        xacThucService.xacThucEmail(token);

        assertThat(taiKhoanRepository.findById(ketQua.id()).orElseThrow().getTrangThai())
                .isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertMaLoi(() -> xacThucService.xacThucEmail(token), MaLoi.LIEN_KET_KHONG_HOP_LE);
    }

    @Test
    void emailDaKichHoatThiBao409() {
        String email = emailMoi();
        dangKy(email, soDienThoai(), "123456");
        xacThucService.xacThucEmail(tokenCuoiCung());

        assertMaLoi(() -> dangKy(email.toUpperCase(), soDienThoai(), "654321"), MaLoi.EMAIL_DA_TON_TAI);
    }

    private TaiKhoanResponse dangKy(String email, String soDienThoai, String matKhau) {
        return xacThucService.dangKy(new DangKyRequest("Bệnh nhân test", email, matKhau, soDienThoai, null));
    }

    private List<EmailXacThucEvent> cacEmailDaPhat() {
        return applicationEvents.stream(EmailXacThucEvent.class).toList();
    }

    private String tokenCuoiCung() {
        List<EmailXacThucEvent> events = cacEmailDaPhat();
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
        return "bn-" + hauTo() + "@example.com";
    }

    private static String soDienThoai() {
        return "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private static String hauTo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

}
