package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.client.ThongTinGoogle;
import iuh.fit.se.eclinic.identity.client.XacMinhTokenGoogle;
import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapGoogleService;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.XacThucService;

/**
 * Đăng nhập Google với MySQL + Redis thật. Việc kiểm tra ID token được giả lập (XacMinhTokenGoogleTest kiểm tra riêng):
 * mỗi test quy định thông tin Google trả về.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@RecordApplicationEvents
class DangNhapGoogleServiceTest {

    private static final String MAT_KHAU = "123456";
    private static final String THIET_BI = "JUnit/5 (test)";

    @MockitoBean XacMinhTokenGoogle xacMinhTokenGoogle;

    @Autowired DangNhapGoogleService dangNhapGoogleService;
    @Autowired DangNhapService dangNhapService;
    @Autowired XacThucService xacThucService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired ApplicationEvents applicationEvents;

    @Test
    void chuaCoTaiKhoanThiTaoBenhNhanDaKichHoatKhongMatKhau() {
        ThongTinGoogle google = new ThongTinGoogle(subMoi(), gmailMoi(), null, "Nguyễn Văn Google",
                "https://lh3.googleusercontent.com/a/anh");

        DangNhapResponse ketQua = dangNhapGoogle(google);

        TaiKhoan taiKhoan = taiKhoanRepository.findById(ketQua.taiKhoan().id()).orElseThrow();
        assertThat(taiKhoan.getEmail()).isEqualTo(google.email());
        assertThat(taiKhoan.getGoogleId()).isEqualTo(google.sub());
        assertThat(taiKhoan.getVaiTro()).isEqualTo(VaiTro.BENH_NHAN);
        assertThat(taiKhoan.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(taiKhoan.getMatKhauHash()).isNull();
        assertThat(taiKhoan.getSoDienThoai()).isNull();
        assertThat(taiKhoan.getHoTen()).isEqualTo("Nguyễn Văn Google");
        assertThat(taiKhoan.getAnhDaiDien()).isEqualTo(google.anh());
        assertThat(jwtDecoder.decode(ketQua.accessToken()).getSubject()).isEqualTo(taiKhoan.getId().toString());
        assertThat(dangNhapService.lamMoi(ketQua.refreshToken()).accessToken()).isNotBlank();

        assertThat(dangNhapGoogle(google).taiKhoan().id()).isEqualTo(taiKhoan.getId());
    }

    @Test
    void khongCoTenThiLayPhanTruocAcuaEmail() {
        String email = gmailMoi();
        DangNhapResponse ketQua = dangNhapGoogle(new ThongTinGoogle(subMoi(), email, null, " ", null));

        assertThat(ketQua.taiKhoan().hoTen()).isEqualTo(email.substring(0, email.indexOf('@')));
    }

    @Test
    void timTheoSubKhiEmailGoogleDaDoiVaKhongDongBoEmail() {
        String sub = subMoi();
        String emailCu = gmailMoi();
        Long id = dangNhapGoogle(new ThongTinGoogle(sub, emailCu, null, "A", null)).taiKhoan().id();
        String emailMoi = gmailMoi();

        assertThat(dangNhapGoogle(new ThongTinGoogle(sub, emailMoi, null, "A", null)).taiKhoan().id()).isEqualTo(id);
        assertThat(taiKhoanRepository.findById(id).orElseThrow().getEmail()).isEqualTo(emailCu);
        assertThat(taiKhoanRepository.findByEmail(emailMoi)).isEmpty();
    }

    @Test
    void taiKhoanMatKhauDaKichHoatDuocLienKetVaVanDangNhapMatKhauDuoc() {
        TaiKhoan taiKhoan = taoTaiKhoan(gmailMoi(), VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT);
        String sub = subMoi();

        DangNhapResponse ketQua = dangNhapGoogle(new ThongTinGoogle(sub, taiKhoan.getEmail(), null, "Tên Google", null));

        TaiKhoan moiNhat = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        assertThat(ketQua.taiKhoan().id()).isEqualTo(taiKhoan.getId());
        assertThat(moiNhat.getGoogleId()).isEqualTo(sub);
        assertThat(moiNhat.getHoTen()).isEqualTo("Bệnh nhân test");
        assertThat(dangNhapMatKhau(taiKhoan.getEmail()).accessToken()).isNotBlank();
    }

    @Test
    void taiKhoanChoXacNhanBiChiemTruocThiGoogleNhanLaiVaBoMatKhauSoDienThoaiLienKet() {
        String email = gmailMoi();
        String soDienThoai = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        // Kẻ xấu đăng ký trước bằng email của nạn nhân
        xacThucService.dangKy(new DangKyRequest("Kẻ chiếm trước", email, MAT_KHAU, soDienThoai));
        List<EmailXacThucEvent> events = applicationEvents.stream(EmailXacThucEvent.class).toList();
        String tokenKichHoat = events.get(events.size() - 1).token();

        DangNhapResponse ketQua = dangNhapGoogle(new ThongTinGoogle(subMoi(), email, null, "Chủ email", null));

        TaiKhoan taiKhoan = taiKhoanRepository.findById(ketQua.taiKhoan().id()).orElseThrow();
        assertThat(taiKhoan.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(taiKhoan.getMatKhauHash()).isNull();
        assertThat(taiKhoan.getSoDienThoai()).isNull();
        assertThat(taiKhoan.getHoTen()).isEqualTo("Chủ email");
        assertMaLoi(() -> dangNhapMatKhau(email), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        assertMaLoi(() -> xacThucService.xacThucEmail(tokenKichHoat), MaLoi.LIEN_KET_KHONG_HOP_LE);
        // Số điện thoại chưa xác minh được trả lại
        assertThat(taiKhoanRepository.existsBySoDienThoai(soDienThoai)).isFalse();
    }

    @Test
    void emailGoogleKhongDamBaoTrungTaiKhoanBao409TruKhiCoHd() {
        TaiKhoan taiKhoan = taoTaiKhoan(emailKhacMoi(), VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT);

        assertMaLoi(() -> dangNhapGoogle(new ThongTinGoogle(subMoi(), taiKhoan.getEmail(), null, "X", null)),
                MaLoi.EMAIL_DA_DANG_KY_MAT_KHAU);
        assertThat(taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getGoogleId()).isNull();

        String sub = subMoi();
        dangNhapGoogle(new ThongTinGoogle(sub, taiKhoan.getEmail(), "example.com", "X", null));
        assertThat(taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getGoogleId()).isEqualTo(sub);
    }

    @Test
    void emailChuaCoTaiKhoanKhongPhaiGmailVanDuocTao() {
        String email = emailKhacMoi();

        DangNhapResponse ketQua = dangNhapGoogle(new ThongTinGoogle(subMoi(), email, null, "Y", null));

        assertThat(ketQua.taiKhoan().email()).isEqualTo(email);
    }

    @Test
    void emailDaLienKetVoiTaiKhoanGoogleKhacBao409() {
        String email = gmailMoi();
        dangNhapGoogle(new ThongTinGoogle(subMoi(), email, null, "A", null));

        assertMaLoi(() -> dangNhapGoogle(new ThongTinGoogle(subMoi(), email, null, "A", null)),
                MaLoi.EMAIL_DA_DANG_KY_MAT_KHAU);
    }

    @Test
    void bacSiVaQuanTriVienKhongDuocDangNhapGoogle() {
        for (VaiTro vaiTro : List.of(VaiTro.BAC_SI, VaiTro.QUAN_TRI_VIEN)) {
            TaiKhoan taiKhoan = taoTaiKhoan(gmailMoi(), vaiTro, TrangThaiTaiKhoan.DA_KICH_HOAT);

            assertMaLoi(() -> dangNhapGoogle(new ThongTinGoogle(subMoi(), taiKhoan.getEmail(), null, "Z", null)),
                    MaLoi.DANG_NHAP_GOOGLE_KHONG_HO_TRO);
            assertThat(taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getGoogleId()).isNull();
        }

        // Kể cả khi (vì lý do nào đó) đã có google_id
        TaiKhoan bacSi = taoTaiKhoan(gmailMoi(), VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT);
        bacSi.setGoogleId(subMoi());
        taiKhoanRepository.save(bacSi);
        assertMaLoi(() -> dangNhapGoogle(new ThongTinGoogle(bacSi.getGoogleId(), bacSi.getEmail(), null, "Z", null)),
                MaLoi.DANG_NHAP_GOOGLE_KHONG_HO_TRO);
    }

    @Test
    void taiKhoanBiVoHieuHoaBao403QuaCaHaiCachTim() {
        TaiKhoan theoEmail = taoTaiKhoan(gmailMoi(), VaiTro.BENH_NHAN, TrangThaiTaiKhoan.VO_HIEU_HOA);
        assertMaLoi(() -> dangNhapGoogle(new ThongTinGoogle(subMoi(), theoEmail.getEmail(), null, "V", null)),
                MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertThat(taiKhoanRepository.findById(theoEmail.getId()).orElseThrow().getGoogleId()).isNull();

        String sub = subMoi();
        Long id = dangNhapGoogle(new ThongTinGoogle(sub, gmailMoi(), null, "V", null)).taiKhoan().id();
        TaiKhoan daLienKet = taiKhoanRepository.findById(id).orElseThrow();
        daLienKet.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoanRepository.save(daLienKet);
        assertMaLoi(() -> dangNhapGoogle(new ThongTinGoogle(sub, daLienKet.getEmail(), null, "V", null)),
                MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
    }

    @Test
    @Timeout(30)
    void haiRequestDangNhapLanDauCungLucChiTaoMotTaiKhoan() throws Exception {
        ThongTinGoogle google = new ThongTinGoogle(subMoi(), gmailMoi(), null, "Song song", null);
        when(xacMinhTokenGoogle.xacMinh(anyString())).thenReturn(google);
        CountDownLatch batDau = new CountDownLatch(1);
        Callable<DangNhapResponse> dangNhap = () -> {
            batDau.await();
            return dangNhapGoogleService.dangNhap("id-token", THIET_BI);
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<DangNhapResponse> thanhCong = new ArrayList<>();
        List<Throwable> loi = new ArrayList<>();
        try {
            List<Future<DangNhapResponse>> ketQua = List.of(executor.submit(dangNhap), executor.submit(dangNhap));
            batDau.countDown();
            for (Future<DangNhapResponse> f : ketQua) {
                try {
                    thanhCong.add(f.get(20, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    loi.add(e.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
        }

        // Không khẳng định request nào thắng: request sau hoặc thấy tài khoản đã commit, hoặc vi phạm UNIQUE (409)
        assertThat(thanhCong).isNotEmpty();
        assertThat(loi).allMatch(DataIntegrityViolationException.class::isInstance);
        Long id = taiKhoanRepository.findByGoogleId(google.sub()).orElseThrow().getId();
        assertThat(thanhCong).allMatch(r -> r.taiKhoan().id().equals(id));
    }

    private DangNhapResponse dangNhapGoogle(ThongTinGoogle google) {
        when(xacMinhTokenGoogle.xacMinh(anyString())).thenReturn(google);
        return dangNhapGoogleService.dangNhap("id-token", THIET_BI);
    }

    private DangNhapResponse dangNhapMatKhau(String email) {
        return dangNhapService.dangNhap(new DangNhapRequest(email, MAT_KHAU), THIET_BI);
    }

    private TaiKhoan taoTaiKhoan(String email, VaiTro vaiTro, TrangThaiTaiKhoan trangThai) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Bệnh nhân test");
        taiKhoan.setEmail(email);
        taiKhoan.setMatKhauHash(passwordEncoder.encode(MAT_KHAU));
        taiKhoan.setVaiTro(vaiTro);
        taiKhoan.setTrangThai(trangThai);
        return taiKhoanRepository.save(taiKhoan);
    }

    private static void assertMaLoi(Runnable hanhDong, MaLoi maLoi) {
        assertThatThrownBy(hanhDong::run)
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

    private static String subMoi() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L));
    }

    private static String gmailMoi() {
        return "gg-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
    }

    private static String emailKhacMoi() {
        return "gg-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

}
