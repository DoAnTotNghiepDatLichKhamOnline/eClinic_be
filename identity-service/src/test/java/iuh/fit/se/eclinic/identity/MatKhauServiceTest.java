package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.event.ApplicationEvents;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.JwtConfig;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;

/** Quên / đặt lại mật khẩu và đổi mật khẩu khi đang đăng nhập, với MySQL + Redis thật. */
class MatKhauServiceTest extends KiemThuTichHop {

    private static final String MAT_KHAU_CU = "matkhau-cu";
    private static final String MAT_KHAU_MOI = "matkhau-moi";

    @Autowired MatKhauService matKhauService;
    @Autowired DangNhapService dangNhapService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired ApplicationEvents applicationEvents;

    @Test
    void emailKhongTonTaiChuaKichHoatHoacBiVoHieuHoaKhongGui() {
        matKhauService.quenMatKhau(emailMoi());
        matKhauService.quenMatKhau(taoTaiKhoan(TrangThaiTaiKhoan.CHO_XAC_NHAN).getEmail());
        matKhauService.quenMatKhau(taoTaiKhoan(TrangThaiTaiKhoan.VO_HIEU_HOA).getEmail());

        assertThat(cacEmailDatLai()).isEmpty();
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
                .containsExactly(new EmailDoiMatKhauEvent(taiKhoan.getEmail(), taiKhoan.getHoTen(), NgonNgu.VI));
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
                .containsExactly(new EmailDoiMatKhauEvent(taiKhoan.getEmail(), taiKhoan.getHoTen(), NgonNgu.VI));
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
