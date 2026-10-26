package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanResponse;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.XacThucService;

/**
 * Đăng ký / kích hoạt với MySQL + Redis thật. Không gửi email: token lấy từ event đã phát
 * (EmailService thật chạy ở chế độ console và chỉ ghi log).
 * Thời gian chờ gửi lại rút còn 5 giây để test được cả lúc còn và lúc hết thời gian chờ.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@RecordApplicationEvents
@TestPropertySource(properties = "app.lien-ket.thoi-gian-cho-gui-lai=5s")
class XacThucServiceTest {

    @Autowired XacThucService xacThucService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StringRedisTemplate redisTemplate;
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

    @Test
    void trungSoDienThoaiThiBao409() {
        String soDienThoai = soDienThoai();
        dangKy(emailMoi(), soDienThoai, "123456");

        assertMaLoi(() -> dangKy(emailMoi(), soDienThoai, "123456"), MaLoi.SO_DIEN_THOAI_DA_TON_TAI);
    }

    @Test
    void dangKyLaiEmailChoXacThucTrongThoiGianChoBao429SauDoGhiDe() throws InterruptedException {
        String email = emailMoi();
        TaiKhoanResponse lanDau = dangKy(email, soDienThoai(), "matkhau-cu");
        String tokenCu = tokenCuoiCung();

        assertMaLoi(() -> dangKy(email, soDienThoai(), "matkhau-moi"), MaLoi.GUI_LAI_QUA_NHANH);
        assertThat(passwordEncoder.matches("matkhau-cu", matKhauHash(lanDau.id()))).isTrue();

        choHetThoiGianCho(lanDau.id());
        String soDienThoaiMoi = soDienThoai();
        TaiKhoanResponse lanHai = dangKy(email, soDienThoaiMoi, "matkhau-moi");

        assertThat(lanHai.id()).isEqualTo(lanDau.id());
        assertThat(lanHai.soDienThoai()).isEqualTo(soDienThoaiMoi);
        assertThat(passwordEncoder.matches("matkhau-moi", matKhauHash(lanDau.id()))).isTrue();
        assertMaLoi(() -> xacThucService.xacThucEmail(tokenCu), MaLoi.LIEN_KET_KHONG_HOP_LE);
        xacThucService.xacThucEmail(tokenCuoiCung());
    }

    @Test
    void lienKetCuaTaiKhoanBiVoHieuHoaBao403() {
        TaiKhoanResponse ketQua = dangKy(emailMoi(), soDienThoai(), "123456");
        TaiKhoan taiKhoan = taiKhoanRepository.findById(ketQua.id()).orElseThrow();
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoanRepository.save(taiKhoan);

        assertMaLoi(() -> xacThucService.xacThucEmail(tokenCuoiCung()), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
    }

    @Test
    void guiLaiChoTaiKhoanDaKichHoatKhongGui() throws InterruptedException {
        String email = emailMoi();
        TaiKhoanResponse ketQua = dangKy(email, soDienThoai(), "123456");
        xacThucService.xacThucEmail(tokenCuoiCung());
        choHetThoiGianCho(ketQua.id());
        applicationEvents.clear();

        xacThucService.guiLaiXacThuc(email);

        assertThat(cacEmailDaPhat()).isEmpty();
    }

    @Test
    void guiLaiChoEmailKhongTonTaiKhongLoiKhongGui() {
        xacThucService.guiLaiXacThuc(emailMoi());

        assertThat(cacEmailDaPhat()).isEmpty();
    }

    @Test
    void guiLaiTrongThoiGianChoKhongGuiThem() {
        String email = emailMoi();
        dangKy(email, soDienThoai(), "123456");

        xacThucService.guiLaiXacThuc(email);

        assertThat(cacEmailDaPhat()).hasSize(1);
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

    private String matKhauHash(Long id) {
        return taiKhoanRepository.findById(id).orElseThrow().getMatKhauHash();
    }

    /** Chờ khoá chờ (5 giây) trên Redis hết hạn thay vì ngủ cứng. */
    private void choHetThoiGianCho(Long idTaiKhoan) throws InterruptedException {
        String khoa = "lien-ket:cho:XAC_THUC_EMAIL:" + idTaiKhoan;
        long hanChot = System.currentTimeMillis() + 8_000;
        while (Boolean.TRUE.equals(redisTemplate.hasKey(khoa))) {
            assertThat(System.currentTimeMillis()).as("khoá chờ phải hết hạn sau 5 giây").isLessThan(hanChot);
            Thread.sleep(200);
        }
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
