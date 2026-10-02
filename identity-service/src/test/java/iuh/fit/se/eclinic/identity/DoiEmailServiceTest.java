package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import iuh.fit.se.eclinic.identity.dto.request.DoiEmailRequest;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.dto.response.YeuCauDoiEmailResponse;
import iuh.fit.se.eclinic.identity.event.EmailDaDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacNhanDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailYeuCauDoiEmailEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.DoiEmailService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;

/**
 * Đổi email đăng nhập với MySQL + Redis thật. Không gửi email: token lấy từ event đã phát (EmailService thật chạy ở chế độ
 * console và chỉ ghi log).
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@RecordApplicationEvents
class DoiEmailServiceTest {

    private static final String MAT_KHAU = "matkhau-cu";

    @Autowired DoiEmailService doiEmailService;
    @Autowired MatKhauService matKhauService;
    @Autowired DangNhapService dangNhapService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired ApplicationEvents applicationEvents;

    @Test
    void yeuCauGuiLienKetToiEmailMoiThongBaoToiEmailCuVaChuaDoiGi() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        String emailMoi = emailMoi();

        YeuCauDoiEmailResponse ketQua = yeuCau(taiKhoan, "  " + emailMoi.toUpperCase() + " ", MAT_KHAU);

        assertThat(ketQua.emailMoi()).isEqualTo(emailMoi);
        List<EmailXacNhanDoiEmailEvent> xacNhan = applicationEvents.stream(EmailXacNhanDoiEmailEvent.class).toList();
        assertThat(xacNhan).hasSize(1);
        assertThat(xacNhan.get(0).emailMoi()).isEqualTo(emailMoi);
        assertThat(xacNhan.get(0).toString()).doesNotContain(xacNhan.get(0).token());
        assertThat(applicationEvents.stream(EmailYeuCauDoiEmailEvent.class).toList())
                .containsExactly(new EmailYeuCauDoiEmailEvent(taiKhoan.getEmail(), taiKhoan.getHoTen(), emailMoi));
        // Chưa xác nhận: email trong DB và việc đăng nhập không đổi
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(taiKhoan.getEmail());
        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU).accessToken()).isNotBlank();
        assertMaLoi(() -> dangNhap(emailMoi, MAT_KHAU), MaLoi.SAI_THONG_TIN_DANG_NHAP);
    }

    @Test
    void saiMatKhauBao400KhongGuiGiVaKhongLoEmailDaDangKy() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        TaiKhoan nguoiKhac = taoTaiKhoan();

        // Email của người khác + sai mật khẩu: chỉ báo sai mật khẩu
        assertMaLoi(() -> yeuCau(taiKhoan, nguoiKhac.getEmail(), "sai-mat-khau"), MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        assertMaLoi(() -> yeuCau(taiKhoan, emailMoi(), "sai-mat-khau"), MaLoi.MAT_KHAU_CU_KHONG_DUNG);

        assertThat(applicationEvents.stream(EmailXacNhanDoiEmailEvent.class).toList()).isEmpty();
        assertThat(applicationEvents.stream(EmailYeuCauDoiEmailEvent.class).toList()).isEmpty();
        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isNull();
    }

    @Test
    void sai5LanThiKhoaCaDoiEmailLanDoiMatKhauNhungVanDangNhapDuoc() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        for (int i = 0; i < 5; i++) {
            assertMaLoi(() -> yeuCau(taiKhoan, emailMoi(), "sai-mat-khau"), MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        }

        // Chung 1 bộ đếm: mật khẩu đúng cũng bị từ chối ở cả 2 chức năng
        assertMaLoi(() -> yeuCau(taiKhoan, emailMoi(), MAT_KHAU), MaLoi.SAI_MAT_KHAU_QUA_NHIEU);
        assertMaLoi(() -> matKhauService.doiMatKhau(taiKhoan.getId(), null,
                new DoiMatKhauRequest(MAT_KHAU, "matkhau-moi")), MaLoi.SAI_MAT_KHAU_QUA_NHIEU);
        assertThat(dangNhap(taiKhoan.getEmail(), MAT_KHAU).accessToken()).isNotBlank();
    }

    @Test
    void lanSaiCuaDoiMatKhauCungTinhChoDoiEmailVaNhapDungThiXoaBoDem() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        for (int i = 0; i < 4; i++) {
            assertMaLoi(() -> matKhauService.doiMatKhau(taiKhoan.getId(), null,
                    new DoiMatKhauRequest("sai-mat-khau", "matkhau-moi")), MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        }
        yeuCau(taiKhoan, emailMoi(), MAT_KHAU);

        // Nếu bộ đếm còn 4 thì lần sai này là lần thứ 5 và lần yêu cầu sau đó bị SAI_MAT_KHAU_QUA_NHIEU
        assertMaLoi(() -> yeuCau(taiKhoan, emailMoi(), "sai-mat-khau"), MaLoi.MAT_KHAU_CU_KHONG_DUNG);
        assertMaLoi(() -> yeuCau(taiKhoan, emailMoi(), MAT_KHAU), MaLoi.GUI_LAI_QUA_NHANH);
    }

    @Test
    void taiKhoanChiCoGoogleBao409() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        taiKhoan.setMatKhauHash(null);
        taiKhoan.setGoogleId("google-" + UUID.randomUUID());
        taiKhoanRepository.save(taiKhoan);

        assertMaLoi(() -> yeuCau(taiKhoan, emailMoi(), MAT_KHAU), MaLoi.TAI_KHOAN_CHUA_CO_MAT_KHAU);
        assertThat(applicationEvents.stream(EmailXacNhanDoiEmailEvent.class).toList()).isEmpty();
    }

    @Test
    void emailMoiTrungEmailHienTaiBao400KeCaKhacHoaThuong() {
        TaiKhoan taiKhoan = taoTaiKhoan();

        assertMaLoi(() -> yeuCau(taiKhoan, taiKhoan.getEmail(), MAT_KHAU), MaLoi.EMAIL_MOI_TRUNG_EMAIL_CU);
        assertMaLoi(() -> yeuCau(taiKhoan, " " + taiKhoan.getEmail().toUpperCase() + " ", MAT_KHAU),
                MaLoi.EMAIL_MOI_TRUNG_EMAIL_CU);
    }

    @Test
    void emailMoiDaThuocTaiKhoanKhacBao409KeCaKhacHoaThuongVaChuaKichHoat() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        TaiKhoan nguoiKhac = taoTaiKhoan();
        TaiKhoan chuaKichHoat = taoTaiKhoan(TrangThaiTaiKhoan.CHO_XAC_NHAN);

        assertMaLoi(() -> yeuCau(taiKhoan, nguoiKhac.getEmail(), MAT_KHAU), MaLoi.EMAIL_DA_TON_TAI);
        assertMaLoi(() -> yeuCau(taiKhoan, "  " + nguoiKhac.getEmail().toUpperCase() + " ", MAT_KHAU),
                MaLoi.EMAIL_DA_TON_TAI);
        assertMaLoi(() -> yeuCau(taiKhoan, chuaKichHoat.getEmail(), MAT_KHAU), MaLoi.EMAIL_DA_TON_TAI);
        assertThat(applicationEvents.stream(EmailXacNhanDoiEmailEvent.class).toList()).isEmpty();
        // Bị từ chối trước khi giữ khoá chờ: yêu cầu hợp lệ ngay sau đó vẫn gửi được
        assertThat(yeuCau(taiKhoan, emailMoi(), MAT_KHAU).emailMoi()).isNotBlank();
    }

    @Test
    void yeuCauLanHaiTrongThoiGianChoBao429VaLienKetDauVanDungDuoc() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        String emailMoi = emailMoi();
        yeuCau(taiKhoan, emailMoi, MAT_KHAU);

        assertMaLoi(() -> yeuCau(taiKhoan, emailMoi(), MAT_KHAU), MaLoi.GUI_LAI_QUA_NHANH);

        assertThat(applicationEvents.stream(EmailXacNhanDoiEmailEvent.class).toList()).hasSize(1);
        doiEmailService.xacNhan(tokenCuoiCung());
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(emailMoi);
    }

    @Test
    void yeuCauMoiLamLienKetCuHetHieuLucVaChiApDungEmailCuaLienKetMoi() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        String emailA = emailMoi();
        String emailB = emailMoi();
        yeuCau(taiKhoan, emailA, MAT_KHAU);
        String tokenA = tokenCuoiCung();
        hetThoiGianCho(taiKhoan);
        yeuCau(taiKhoan, emailB, MAT_KHAU);
        String tokenB = tokenCuoiCung();

        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isEqualTo(new YeuCauDoiEmailResponse(emailB));
        assertMaLoi(() -> doiEmailService.xacNhan(tokenA), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(taiKhoan.getEmail());
        doiEmailService.xacNhan(tokenB);
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(emailB);
    }

    @Test
    void xemYeuCauDangChoRoiHuyThiLienKetHetHieuLuc() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        String emailMoi = emailMoi();
        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isNull();

        yeuCau(taiKhoan, emailMoi, MAT_KHAU);
        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isEqualTo(new YeuCauDoiEmailResponse(emailMoi));

        doiEmailService.huy(taiKhoan.getId());
        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isNull();
        assertMaLoi(() -> doiEmailService.xacNhan(tokenCuoiCung()), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(taiKhoan.getEmail());
        // Huỷ khi không có yêu cầu nào cũng không báo lỗi
        doiEmailService.huy(taiKhoan.getId());
    }

    @Test
    void xacNhanDoiEmailDangXuatMoiThietBiHuyLienKetDatLaiVaGuiThongBao() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        String googleId = "google-" + UUID.randomUUID();
        taiKhoan.setGoogleId(googleId);
        taiKhoanRepository.save(taiKhoan);
        String emailCu = taiKhoan.getEmail();
        String emailMoi = emailMoi();
        DangNhapResponse mayA = dangNhap(emailCu, MAT_KHAU);
        DangNhapResponse mayB = dangNhap(emailCu, MAT_KHAU);
        matKhauService.quenMatKhau(emailCu);
        String tokenDatLai = applicationEvents.stream(EmailDatLaiMatKhauEvent.class).toList().get(0).token();
        // Có người từng thử đăng nhập sai bằng email mới tới mức bị khoá
        for (int i = 0; i < 5; i++) {
            assertMaLoi(() -> dangNhap(emailMoi, "sai-mat-khau"), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        }
        yeuCau(taiKhoan, emailMoi, MAT_KHAU);

        doiEmailService.xacNhan(tokenCuoiCung());

        TaiKhoan sauKhiDoi = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        assertThat(sauKhiDoi.getEmail()).isEqualTo(emailMoi);
        // Chỉ đổi email: mật khẩu, liên kết Google, trạng thái giữ nguyên
        assertThat(sauKhiDoi.getGoogleId()).isEqualTo(googleId);
        assertThat(sauKhiDoi.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertMaLoi(() -> dangNhapService.lamMoi(mayA.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertMaLoi(() -> dangNhapService.lamMoi(mayB.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertMaLoi(() -> dangNhap(emailCu, MAT_KHAU), MaLoi.SAI_THONG_TIN_DANG_NHAP);
        assertThat(dangNhap(emailMoi, MAT_KHAU).taiKhoan().email()).isEqualTo(emailMoi);
        // Liên kết đặt lại mật khẩu đã gửi tới email cũ không còn dùng được
        assertMaLoi(() -> matKhauService.datLaiMatKhau(tokenDatLai, "matkhau-moi"), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(applicationEvents.stream(EmailDaDoiEmailEvent.class).toList())
                .containsExactly(new EmailDaDoiEmailEvent(emailCu, taiKhoan.getHoTen(), emailMoi));
        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isNull();
    }

    @Test
    void tokenChiDungMotLanVaTokenRacBao410() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        String emailMoi = emailMoi();
        yeuCau(taiKhoan, emailMoi, MAT_KHAU);
        String token = tokenCuoiCung();
        doiEmailService.xacNhan(token);

        assertMaLoi(() -> doiEmailService.xacNhan(token), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertMaLoi(() -> doiEmailService.xacNhan("token-rac"), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(emailMoi);
        assertThat(applicationEvents.stream(EmailDaDoiEmailEvent.class).toList()).hasSize(1);
    }

    @Test
    void emailMoiBiTaiKhoanKhacLayTruocKhiXacNhanBao409VaKhongDoiGi() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        String emailMoi = emailMoi();
        String refreshToken = dangNhap(taiKhoan.getEmail(), MAT_KHAU).refreshToken();
        yeuCau(taiKhoan, emailMoi, MAT_KHAU);
        taoTaiKhoan(TrangThaiTaiKhoan.CHO_XAC_NHAN, emailMoi);

        assertMaLoi(() -> doiEmailService.xacNhan(tokenCuoiCung()), MaLoi.EMAIL_DA_TON_TAI);

        assertThat(emailTrongDb(taiKhoan)).isEqualTo(taiKhoan.getEmail());
        assertThat(dangNhapService.lamMoi(refreshToken).accessToken()).isNotBlank();
        assertThat(applicationEvents.stream(EmailDaDoiEmailEvent.class).toList()).isEmpty();
    }

    @Test
    void taiKhoanBiVoHieuHoaSauKhiYeuCauBao403() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        yeuCau(taiKhoan, emailMoi(), MAT_KHAU);
        TaiKhoan moiNhat = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        moiNhat.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoanRepository.save(moiNhat);

        assertMaLoi(() -> doiEmailService.xacNhan(tokenCuoiCung()), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(taiKhoan.getEmail());
    }

    @Test
    void doiMatKhauHuyYeuCauDoiEmailDangCho() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        yeuCau(taiKhoan, emailMoi(), MAT_KHAU);

        matKhauService.doiMatKhau(taiKhoan.getId(), null, new DoiMatKhauRequest(MAT_KHAU, "matkhau-moi"));

        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isNull();
        assertMaLoi(() -> doiEmailService.xacNhan(tokenCuoiCung()), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(taiKhoan.getEmail());
    }

    @Test
    void datLaiMatKhauHuyYeuCauDoiEmailDangCho() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        yeuCau(taiKhoan, emailMoi(), MAT_KHAU);
        matKhauService.quenMatKhau(taiKhoan.getEmail());
        String tokenDatLai = applicationEvents.stream(EmailDatLaiMatKhauEvent.class).toList().get(0).token();

        matKhauService.datLaiMatKhau(tokenDatLai, "matkhau-moi");

        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isNull();
        assertMaLoi(() -> doiEmailService.xacNhan(tokenCuoiCung()), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(emailTrongDb(taiKhoan)).isEqualTo(taiKhoan.getEmail());
    }

    @Test
    void taiKhoanBiVoHieuHoaBao403TaiKhoanKhongConBao401() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.VO_HIEU_HOA);

        assertMaLoi(() -> yeuCau(taiKhoan, emailMoi(), MAT_KHAU), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> doiEmailService.layYeuCauDangCho(taiKhoan.getId()), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> doiEmailService.huy(taiKhoan.getId()), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> doiEmailService.yeuCau(Long.MAX_VALUE, new DoiEmailRequest(emailMoi(), MAT_KHAU)),
                MaLoi.CHUA_DANG_NHAP);
    }

    private YeuCauDoiEmailResponse yeuCau(TaiKhoan taiKhoan, String emailMoi, String matKhau) {
        return doiEmailService.yeuCau(taiKhoan.getId(), new DoiEmailRequest(emailMoi, matKhau));
    }

    /** Xoá khoá chờ giữa 2 lần gửi (thay cho việc đợi 60 giây). */
    private void hetThoiGianCho(TaiKhoan taiKhoan) {
        assertThat(redisTemplate.delete("lien-ket:cho:DOI_EMAIL:" + taiKhoan.getId())).isTrue();
    }

    private String emailTrongDb(TaiKhoan taiKhoan) {
        return taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getEmail();
    }

    private TaiKhoan taoTaiKhoan() {
        return taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
    }

    private TaiKhoan taoTaiKhoan(TrangThaiTaiKhoan trangThai) {
        return taoTaiKhoan(trangThai, emailMoi());
    }

    private TaiKhoan taoTaiKhoan(TrangThaiTaiKhoan trangThai, String email) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Bệnh nhân test");
        taiKhoan.setEmail(email);
        taiKhoan.setMatKhauHash(passwordEncoder.encode(MAT_KHAU));
        taiKhoan.setVaiTro(VaiTro.BENH_NHAN);
        taiKhoan.setTrangThai(trangThai);
        return taiKhoanRepository.save(taiKhoan);
    }

    private DangNhapResponse dangNhap(String email, String matKhau) {
        return dangNhapService.dangNhap(new DangNhapRequest(email, matKhau), "JUnit");
    }

    /** Token của liên kết xác nhận gửi gần nhất trong test này. */
    private String tokenCuoiCung() {
        List<EmailXacNhanDoiEmailEvent> events = applicationEvents.stream(EmailXacNhanDoiEmailEvent.class).toList();
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
        return "de-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";
    }

}
