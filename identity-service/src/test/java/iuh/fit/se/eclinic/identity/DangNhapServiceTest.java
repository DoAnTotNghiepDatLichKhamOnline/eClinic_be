package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.JwtConfig;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.job.DonPhienHetHanJob;
import iuh.fit.se.eclinic.identity.repository.RefreshTokenRepository;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.util.TokenNgauNhien;

/**
 * Đăng nhập / làm mới phiên / đăng xuất với MySQL + Redis thật. Tài khoản tạo thẳng qua repository.
 * Ân hạn dùng lại refresh token rút còn 5 giây; "quá ân hạn" được giả lập bằng cách lùi ngay_thu_hoi 1 phút.
 * Mỗi lần đăng nhập là 1 phiên có mã riêng (ma_phien), giữ nguyên qua các lần làm mới và nằm trong access token.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@TestPropertySource(properties = "app.dang-nhap.an-han-dung-lai=5s")
class DangNhapServiceTest {

    private static final String MAT_KHAU = "123456";
    private static final String THIET_BI = "JUnit/5 (test)";

    @Autowired DangNhapService dangNhapService;
    @Autowired RefreshTokenService refreshTokenService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired DonPhienHetHanJob donPhienHetHanJob;

    @Test
    void dangNhapThanhCongCapTokenVaChiLuuHash() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);

        DangNhapResponse ketQua = dangNhapService.dangNhap(
                new DangNhapRequest("  " + taiKhoan.getEmail().toUpperCase() + " ", MAT_KHAU), THIET_BI);

        Jwt jwt = jwtDecoder.decode(ketQua.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(taiKhoan.getId().toString());
        assertThat(jwt.getClaimAsString(JwtConfig.CLAIM_VAI_TRO)).isEqualTo("BENH_NHAN");
        assertThat(ketQua.loaiToken()).isEqualTo("Bearer");
        assertThat(ketQua.thoiHanAccessToken()).isEqualTo(1800);
        assertThat(ketQua.taiKhoan().id()).isEqualTo(taiKhoan.getId());

        RefreshToken phien = phien(ketQua.refreshToken());
        assertThat(phien.getTokenHash()).matches("[0-9a-f]{64}").isNotEqualTo(ketQua.refreshToken());
        assertThat(phien.getThongTinThietBi()).isEqualTo(THIET_BI);
        assertThat(phien.getNgayThuHoi()).isNull();
        // Phiên có mã riêng, access token mang đúng mã đó
        assertThat(phien.getMaPhien()).matches("[0-9a-f-]{36}");
        assertThat(phien.getNgayDangNhap()).isNotNull();
        assertThat(maPhien(ketQua)).isEqualTo(phien.getMaPhien());
    }

    @Test
    void haiLanDangNhapLaHaiPhienKhacNhau() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);

        DangNhapResponse mayA = dangNhap(taiKhoan.getEmail(), MAT_KHAU);
        DangNhapResponse mayB = dangNhap(taiKhoan.getEmail(), MAT_KHAU);

        assertThat(maPhien(mayA)).isNotEqualTo(maPhien(mayB));
    }

    @Test
    void saiEmailHoacSaiMatKhauCungBao401() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);

        assertMaLoi(() -> dangNhap(emailMoi(), MAT_KHAU), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), "sai-mat-khau"), MaLoi.SAI_THONG_TIN_DANG_NHAP);
    }

    @Test
    void matKhauDungThemKyTuSauByte72Bao401() {
        // BCrypt chỉ đọc 72 byte đầu: không chặn thì "mật khẩu + x" cũng đăng nhập được
        String matKhau72Byte = "a".repeat(72);
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, matKhau72Byte);

        assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), matKhau72Byte + "x"), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        assertThat(dangNhap(taiKhoan.getEmail(), matKhau72Byte).accessToken()).isNotBlank();
    }

    @Test
    void trangThaiTaiKhoanChiBaoKhiMatKhauDung() {
        TaiKhoan choXacThuc = taoTaiKhoan(TrangThaiTaiKhoan.CHO_XAC_NHAN, MAT_KHAU);
        TaiKhoan voHieuHoa = taoTaiKhoan(TrangThaiTaiKhoan.VO_HIEU_HOA, MAT_KHAU);

        assertMaLoi(() -> dangNhap(choXacThuc.getEmail(), MAT_KHAU), MaLoi.TAI_KHOAN_CHUA_XAC_THUC);
        assertMaLoi(() -> dangNhap(choXacThuc.getEmail(), "sai-mat-khau"), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        assertMaLoi(() -> dangNhap(voHieuHoa.getEmail(), MAT_KHAU), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> dangNhap(voHieuHoa.getEmail(), "sai-mat-khau"), MaLoi.SAI_THONG_TIN_DANG_NHAP);
    }

    @Test
    void sai5LanThiKhoaKeCaMatKhauDung() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        saiNhieuLan(taiKhoan.getEmail(), 5);

        assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), MAT_KHAU), MaLoi.DANG_NHAP_SAI_QUA_NHIEU);
    }

    @Test
    void taiKhoanKhongCoMatKhauBao401VaTinhLanSai() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        taiKhoan.setMatKhauHash(null);
        taiKhoan.setGoogleId("google-" + UUID.randomUUID());
        taiKhoanRepository.save(taiKhoan);

        for (int i = 0; i < 5; i++) {
            assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), MAT_KHAU), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        }
        assertMaLoi(() -> dangNhap(taiKhoan.getEmail(), MAT_KHAU), MaLoi.DANG_NHAP_SAI_QUA_NHIEU);
    }

    @Test
    void nhatKyDangNhapSaiChiGhiIdKhongGhiEmailMatKhau(CapturedOutput output) {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        String emailChuaDangKy = emailMoi();
        saiNhieuLan(taiKhoan.getEmail(), 5);
        assertMaLoi(() -> dangNhap(emailChuaDangKy, "mat-khau-bi-mat"), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        TaiKhoan choXacNhan = taoTaiKhoan(TrangThaiTaiKhoan.CHO_XAC_NHAN, MAT_KHAU);
        assertMaLoi(() -> dangNhap(choXacNhan.getEmail(), MAT_KHAU), MaLoi.TAI_KHOAN_CHUA_XAC_THUC);

        assertThat(output)
                .contains("Đăng nhập sai id=" + taiKhoan.getId() + " (lần sai thứ 1)")
                .contains("Khoá đăng nhập id=" + taiKhoan.getId() + " sau 5 lần sai mật khẩu")
                .contains("Đăng nhập sai id=- (lần sai thứ 1)")
                .contains("Từ chối đăng nhập id=" + choXacNhan.getId() + ": tài khoản chưa kích hoạt")
                .doesNotContain(taiKhoan.getEmail())
                .doesNotContain(emailChuaDangKy)
                .doesNotContain("sai-mat-khau")
                .doesNotContain("mat-khau-bi-mat");
    }

    @Test
    void emailKhongTonTaiCungBiKhoa() {
        String email = emailMoi();
        saiNhieuLan(email, 5);

        assertMaLoi(() -> dangNhap(email, MAT_KHAU), MaLoi.DANG_NHAP_SAI_QUA_NHIEU);
    }

    @Test
    void dangNhapThanhCongXoaBoDem() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        saiNhieuLan(taiKhoan.getEmail(), 4);
        dangNhap(taiKhoan.getEmail(), MAT_KHAU);
        saiNhieuLan(taiKhoan.getEmail(), 4);

        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU).accessToken()).isNotBlank();
    }

    @Test
    void lamMoiXoayVongTokenCuHetHieuLuc() {
        DangNhapResponse lanDau = dangNhapTaiKhoanMoi();

        DangNhapResponse lanHai = dangNhapService.lamMoi(lanDau.refreshToken());

        assertThat(lanHai.refreshToken()).isNotEqualTo(lanDau.refreshToken());
        assertThat(phien(lanDau.refreshToken()).getNgayThuHoi()).isNotNull();
        assertThat(phien(lanHai.refreshToken()).getThongTinThietBi()).isEqualTo(THIET_BI);
        assertThat(dangNhapService.lamMoi(lanHai.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void lamMoiGiuNguyenMaPhienVaThoiDiemDangNhap() {
        DangNhapResponse lanDau = dangNhapTaiKhoanMoi();
        RefreshToken dongDau = phien(lanDau.refreshToken());

        DangNhapResponse lanHai = dangNhapService.lamMoi(lanDau.refreshToken());

        RefreshToken dongHai = phien(lanHai.refreshToken());
        assertThat(dongHai.getId()).isNotEqualTo(dongDau.getId());
        assertThat(dongHai.getMaPhien()).isEqualTo(dongDau.getMaPhien());
        assertThat(dongHai.getNgayDangNhap()).isEqualTo(dongDau.getNgayDangNhap());
        assertThat(maPhien(lanHai)).isEqualTo(maPhien(lanDau)).isEqualTo(dongDau.getMaPhien());
    }

    @Test
    void dungLaiTokenCuTrongAnHanChiBao401() {
        DangNhapResponse lanDau = dangNhapTaiKhoanMoi();
        DangNhapResponse lanHai = dangNhapService.lamMoi(lanDau.refreshToken());

        assertMaLoi(() -> dangNhapService.lamMoi(lanDau.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertThat(dangNhapService.lamMoi(lanHai.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void dungLaiTokenCuSauAnHanChiDangXuatPhienDo() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        DangNhapResponse mayA = dangNhap(taiKhoan.getEmail(), MAT_KHAU);
        DangNhapResponse mayB = dangNhap(taiKhoan.getEmail(), MAT_KHAU);
        DangNhapResponse mayAMoi = dangNhapService.lamMoi(mayA.refreshToken());
        RefreshToken tokenCu = phien(mayA.refreshToken());
        tokenCu.setNgayThuHoi(tokenCu.getNgayThuHoi().minusMinutes(1));
        refreshTokenRepository.save(tokenCu);

        assertMaLoi(() -> dangNhapService.lamMoi(mayA.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);

        // Phiên của token bị dùng lại bị đăng xuất (token mới nhất của nó cũng hết hiệu lực); máy B không bị ảnh hưởng
        assertMaLoi(() -> dangNhapService.lamMoi(mayAMoi.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertThat(refreshTokenService.layPhienDangHoatDong(taiKhoan.getId()))
                .extracting(RefreshToken::getMaPhien).containsExactly(maPhien(mayB));
        assertThat(dangNhapService.lamMoi(mayB.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void dungLaiTokenDaDangXuatSauAnHanKhongDangXuatPhienKhac() {
        // Thiết bị đã đăng xuất (hoặc bị đăng xuất từ xa) quay lại với cookie cũ: chỉ 401, không làm văng thiết bị khác
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        DangNhapResponse mayA = dangNhap(taiKhoan.getEmail(), MAT_KHAU);
        DangNhapResponse mayB = dangNhap(taiKhoan.getEmail(), MAT_KHAU);
        dangNhapService.dangXuat(mayA.refreshToken());
        RefreshToken tokenCu = phien(mayA.refreshToken());
        tokenCu.setNgayThuHoi(tokenCu.getNgayThuHoi().minusMinutes(1));
        refreshTokenRepository.save(tokenCu);

        assertMaLoi(() -> dangNhapService.lamMoi(mayA.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);

        assertThat(dangNhapService.lamMoi(mayB.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void tokenDaThuHoiVaHetHanChiBao401KhongThuHoiPhienKhac() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        DangNhapResponse mayA = dangNhap(taiKhoan.getEmail(), MAT_KHAU);
        DangNhapResponse mayB = dangNhap(taiKhoan.getEmail(), MAT_KHAU);
        dangNhapService.dangXuat(mayA.refreshToken());
        RefreshToken tokenCu = phien(mayA.refreshToken());
        tokenCu.setNgayThuHoi(tokenCu.getNgayThuHoi().minusDays(30));
        tokenCu.setNgayHetHan(tokenCu.getNgayThuHoi().plusDays(7));
        refreshTokenRepository.save(tokenCu);

        assertMaLoi(() -> dangNhapService.lamMoi(mayA.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);

        assertThat(dangNhapService.lamMoi(mayB.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    @Timeout(30)
    void haiRequestLamMoiCungLucChiMotThanhCong() throws Exception {
        String refreshToken = dangNhapTaiKhoanMoi().refreshToken();
        CountDownLatch batDau = new CountDownLatch(1);
        Callable<DangNhapResponse> lamMoi = () -> {
            batDau.await();
            return dangNhapService.lamMoi(refreshToken);
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<DangNhapResponse> thanhCong = new ArrayList<>();
        List<Throwable> loi = new ArrayList<>();
        try {
            List<Future<DangNhapResponse>> ketQua = List.of(executor.submit(lamMoi), executor.submit(lamMoi));
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

        assertThat(thanhCong).hasSize(1);
        assertThat(loi).singleElement()
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertThat(dangNhapService.lamMoi(thanhCong.get(0).refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void lamMoiKhiTaiKhoanBiVoHieuHoaBao403VaThuHoiPhien() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        String refreshToken = dangNhap(taiKhoan.getEmail(), MAT_KHAU).refreshToken();
        TaiKhoan moiNhat = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        moiNhat.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoanRepository.save(moiNhat);

        assertMaLoi(() -> dangNhapService.lamMoi(refreshToken), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertThat(phien(refreshToken).getNgayThuHoi()).isNotNull();
    }

    @Test
    void dangXuatThuHoiPhienVaTokenRacKhongLoi() {
        String refreshToken = dangNhapTaiKhoanMoi().refreshToken();

        dangNhapService.dangXuat(refreshToken);
        dangNhapService.dangXuat(refreshToken);
        dangNhapService.dangXuat("token-rac");

        assertThat(phien(refreshToken).getNgayThuHoi()).isNotNull();
        assertMaLoi(() -> dangNhapService.lamMoi(refreshToken), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
    }

    @Test
    void tokenKhongTonTaiBao401() {
        assertMaLoi(() -> dangNhapService.lamMoi("token-rac"), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
    }

    @Test
    void jobChiXoaPhienHetHan() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU);
        String hetHan = refreshTokenService.tao(taiKhoan, THIET_BI, Duration.ofMinutes(-1)).refreshToken();
        String conHan = refreshTokenService.tao(taiKhoan, THIET_BI, Duration.ofDays(1)).refreshToken();

        donPhienHetHanJob.donPhienHetHan();

        assertThat(refreshTokenRepository.findByTokenHash(TokenNgauNhien.bam(hetHan))).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(TokenNgauNhien.bam(conHan))).isPresent();
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

    private DangNhapResponse dangNhapTaiKhoanMoi() {
        return dangNhap(taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT, MAT_KHAU).getEmail(), MAT_KHAU);
    }

    private DangNhapResponse dangNhap(String email, String matKhau) {
        return dangNhapService.dangNhap(new DangNhapRequest(email, matKhau), THIET_BI);
    }

    private void saiNhieuLan(String email, int soLan) {
        for (int i = 0; i < soLan; i++) {
            assertMaLoi(() -> dangNhap(email, "sai-mat-khau"), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        }
    }

    /** Mã phiên trong access token (claim "phien"). */
    private String maPhien(DangNhapResponse ketQua) {
        return jwtDecoder.decode(ketQua.accessToken()).getClaimAsString(JwtConfig.CLAIM_PHIEN);
    }

    private RefreshToken phien(String refreshToken) {
        return refreshTokenRepository.findByTokenHash(TokenNgauNhien.bam(refreshToken)).orElseThrow();
    }

    private static void assertMaLoi(Runnable hanhDong, MaLoi maLoi) {
        assertThatThrownBy(hanhDong::run)
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

    private static String emailMoi() {
        return "dn-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

}
