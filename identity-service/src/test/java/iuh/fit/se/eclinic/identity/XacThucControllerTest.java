package iuh.fit.se.eclinic.identity;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.identity.config.CookiePhienProperties;
import iuh.fit.se.eclinic.identity.config.DangNhapProperties;
import iuh.fit.se.eclinic.identity.controller.CookiePhien;
import iuh.fit.se.eclinic.identity.controller.XacThucController;
import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanResponse;
import iuh.fit.se.eclinic.identity.service.DangNhapGoogleService;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.XacThucService;
import jakarta.servlet.http.Cookie;

/**
 * Tầng web của /api/auth: công khai (không cần token), validate dữ liệu vào, định dạng JSON, cookie refresh token.
 * Service là mock.
 */
@WebMvcTest(XacThucController.class)
@AutoConfigureJson
@Import({ BaoMatConfig.class, CookiePhien.class })
// @WebMvcTest không quét @ConfigurationProperties: CookiePhien cần 2 record này (giá trị mặc định)
@EnableConfigurationProperties({ CookiePhienProperties.class, DangNhapProperties.class })
class XacThucControllerTest {

    private static final String DANG_KY = "/api/auth/register";
    private static final String DANG_NHAP = "/api/auth/login";
    private static final String DANG_NHAP_GOOGLE = "/api/auth/google";
    private static final String LAM_MOI = "/api/auth/refresh-token";
    private static final String DANG_XUAT = "/api/auth/logout";
    private static final long BAY_NGAY = 7 * 24 * 3600;

    @Autowired MockMvc mockMvc;
    @MockitoBean XacThucService xacThucService;
    @MockitoBean DangNhapService dangNhapService;
    @MockitoBean MatKhauService matKhauService;
    @MockitoBean DangNhapGoogleService dangNhapGoogleService;

    @Test
    void dangKyKhongCanDangNhapTraVe201() throws Exception {
        when(xacThucService.dangKy(any(DangKyRequest.class))).thenReturn(new TaiKhoanResponse(7L, "Nguyễn Văn A",
                "a@example.com", "0912345678", VaiTro.BENH_NHAN, TrangThaiTaiKhoan.CHO_XAC_NHAN));

        guiDangKy("a@example.com", "123456", "0912345678")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu.id").value(7))
                .andExpect(jsonPath("$.duLieu.trangThai").value("CHO_XAC_NHAN"))
                .andExpect(jsonPath("$.duLieu.matKhauHash").doesNotExist())
                .andExpect(jsonPath("$.thongDiep").value("Đăng ký thành công, vui lòng kiểm tra email để kích hoạt tài khoản"));
    }

    @Test
    void xacThucVaGuiLaiKhongCanDangNhap() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Xác thực email thành công"));
        verify(xacThucService).xacThucEmail("abc");

        mockMvc.perform(post("/api/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"a@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true));
        verify(xacThucService).guiLaiXacThuc("a@example.com");
    }

    @Test
    void emailSaiDinhDangTraVe400() throws Exception {
        assertLoiTruong(guiDangKy("khong-phai-email", "123456", "0912345678"), "email");
    }

    @Test
    void matKhauDuoi6KyTuTraVe400() throws Exception {
        assertLoiTruong(guiDangKy("a@example.com", "12345", "0912345678"), "matKhau")
                .andExpect(jsonPath("$.chiTiet[0].thongDiep").value("Mật khẩu tối thiểu 6 ký tự"));
    }

    @Test
    void matKhauQua72ByteTraVe400ChuKhongPhai500() throws Exception {
        // 25 ký tự nhưng 75 byte UTF-8: qua được @Size theo ký tự nhưng BCrypt sẽ ném lỗi
        assertLoiTruong(guiDangKy("a@example.com", "ệ".repeat(25), "0912345678"), "matKhau")
                .andExpect(jsonPath("$.chiTiet[0].thongDiep").value("Mật khẩu quá dài (tối đa 72 byte)"));
    }

    @Test
    void soDienThoaiSaiTraVe400() throws Exception {
        assertLoiTruong(guiDangKy("a@example.com", "123456", "12345"), "soDienThoai");
    }

    @Test
    void lienKetHetHanTraVe410() throws Exception {
        doThrow(new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE)).when(xacThucService).xacThucEmail("cu");

        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"cu\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.maLoi").value("LIEN_KET_KHONG_HOP_LE"));
    }

    @Test
    void dangNhapKhongCanTokenVaTruyenUserAgent() throws Exception {
        TaiKhoanResponse taiKhoan = new TaiKhoanResponse(1L, "Quản trị viên", "admin@eclinic.local", null,
                VaiTro.QUAN_TRI_VIEN, TrangThaiTaiKhoan.DA_KICH_HOAT);
        when(dangNhapService.dangNhap(any(DangNhapRequest.class), eq("UA-test")))
                .thenReturn(new DangNhapResponse("access", "refresh", "Bearer", 1800, taiKhoan));

        mockMvc.perform(post(DANG_NHAP).header("User-Agent", "UA-test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"admin@eclinic.local\", \"matKhau\": \"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu.accessToken").value("access"))
                .andExpect(jsonPath("$.duLieu.refreshToken").doesNotExist())
                .andExpect(cookiePhien("refresh", BAY_NGAY))
                .andExpect(jsonPath("$.duLieu.taiKhoan.vaiTro").value("QUAN_TRI_VIEN"))
                .andExpect(jsonPath("$.thongDiep").value("Đăng nhập thành công"));
        verify(dangNhapService).dangNhap(new DangNhapRequest("admin@eclinic.local", "Admin@123"), "UA-test");
    }

    @Test
    void lamMoiVaDangXuatBangBodyKhongCanToken() throws Exception {
        when(dangNhapService.lamMoi("r1")).thenReturn(phienMoi("r1-moi"));

        mockMvc.perform(post(LAM_MOI).contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\": \"r1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu.refreshToken").doesNotExist())
                .andExpect(cookiePhien("r1-moi", BAY_NGAY));
        verify(dangNhapService).lamMoi("r1");

        mockMvc.perform(post(DANG_XUAT).contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\": \"r2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đã đăng xuất"))
                .andExpect(cookiePhien("", 0));
        verify(dangNhapService).dangXuat("r2");
    }

    @Test
    void lamMoiVaDangXuatBangCookieCookieDuocUuTienHonBody() throws Exception {
        when(dangNhapService.lamMoi("c1")).thenReturn(phienMoi("c1-moi"));

        mockMvc.perform(post(LAM_MOI).cookie(new Cookie(CookiePhien.TEN, "c1")))
                .andExpect(status().isOk())
                .andExpect(cookiePhien("c1-moi", BAY_NGAY));
        mockMvc.perform(post(LAM_MOI).cookie(new Cookie(CookiePhien.TEN, "c1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\": \"body\"}"))
                .andExpect(status().isOk());
        verify(dangNhapService, times(2)).lamMoi("c1");

        mockMvc.perform(post(DANG_XUAT).cookie(new Cookie(CookiePhien.TEN, "c2")))
                .andExpect(status().isOk())
                .andExpect(cookiePhien("", 0));
        verify(dangNhapService).dangXuat("c2");
        verifyNoMoreInteractions(dangNhapService);
    }

    @Test
    void thieuTruongDangNhapTraVe400() throws Exception {
        mockMvc.perform(post(DANG_NHAP).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"\", \"matKhau\": \"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("email")));
        verifyNoInteractions(dangNhapService);
    }

    @Test
    void khongCoRefreshTokenThiLamMoiBao401DangXuatVanThanhCong() throws Exception {
        mockMvc.perform(post(LAM_MOI).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("PHIEN_DANG_NHAP_KHONG_HOP_LE"));
        mockMvc.perform(post(LAM_MOI))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(DANG_XUAT).contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\": \" \"}"))
                .andExpect(status().isOk())
                .andExpect(cookiePhien("", 0));
        mockMvc.perform(post(DANG_XUAT))
                .andExpect(status().isOk());
        mockMvc.perform(post(LAM_MOI).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("refreshToken")));
        verifyNoInteractions(dangNhapService);
    }

    @Test
    void saiThongTinTraVe401VaBiKhoaTraVe429() throws Exception {
        when(dangNhapService.dangNhap(any(DangNhapRequest.class), any()))
                .thenThrow(new LoiNghiepVu(MaLoi.SAI_THONG_TIN_DANG_NHAP))
                .thenThrow(new LoiNghiepVu(MaLoi.DANG_NHAP_SAI_QUA_NHIEU));

        guiDangNhap("a@example.com", "sai-mat-khau")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("SAI_THONG_TIN_DANG_NHAP"));
        guiDangNhap("a@example.com", "sai-mat-khau")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.maLoi").value("DANG_NHAP_SAI_QUA_NHIEU"));
    }

    @Test
    void matKhauDangNhapQua72ByteKhongBiChan400() throws Exception {
        // Đăng nhập không kiểm tra quy tắc mật khẩu: mật khẩu dài phải ra 401 như sai mật khẩu, không phải 400
        when(dangNhapService.dangNhap(any(DangNhapRequest.class), any()))
                .thenThrow(new LoiNghiepVu(MaLoi.SAI_THONG_TIN_DANG_NHAP));

        guiDangNhap("a@example.com", "ệ".repeat(25)).andExpect(status().isUnauthorized());
        verify(dangNhapService).dangNhap(eq(new DangNhapRequest("a@example.com", "ệ".repeat(25))), any());
    }

    @Test
    void quenMatKhauLuonTraCungThongBao() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"a@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Nếu email đã đăng ký, liên kết đặt lại mật khẩu đã được gửi"));
        verify(matKhauService).quenMatKhau("a@example.com");
    }

    @Test
    void datLaiMatKhauKhongCanDangNhap() throws Exception {
        guiDatLai("tok", "matkhau-moi")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đặt lại mật khẩu thành công, vui lòng đăng nhập lại"));
        verify(matKhauService).datLaiMatKhau("tok", "matkhau-moi");
    }

    @Test
    void matKhauMoiSaiQuyTacTraVe400KhongDungToken() throws Exception {
        guiDatLai("tok", "12345").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("matKhauMoi")));
        guiDatLai("tok", "ệ".repeat(25)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("matKhauMoi")));
        guiDatLai("", "matkhau-moi").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("token")));
        verifyNoInteractions(matKhauService);
    }

    @Test
    void lienKetDatLaiHetHanTraVe410() throws Exception {
        doThrow(new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE)).when(matKhauService).datLaiMatKhau("cu", "matkhau-moi");

        guiDatLai("cu", "matkhau-moi")
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.maLoi").value("LIEN_KET_KHONG_HOP_LE"));
    }

    @Test
    void dangNhapGoogleKhongCanTokenVaTruyenUserAgent() throws Exception {
        TaiKhoanResponse taiKhoan = new TaiKhoanResponse(9L, "Người Google", "g@gmail.com", null,
                VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT);
        when(dangNhapGoogleService.dangNhap("id-token", "UA-test"))
                .thenReturn(new DangNhapResponse("access", "refresh", "Bearer", 1800, taiKhoan));

        mockMvc.perform(post(DANG_NHAP_GOOGLE).header("User-Agent", "UA-test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu.accessToken").value("access"))
                .andExpect(jsonPath("$.duLieu.taiKhoan.id").value(9))
                .andExpect(jsonPath("$.duLieu.taiKhoan.vaiTro").value("BENH_NHAN"))
                .andExpect(jsonPath("$.thongDiep").value("Đăng nhập Google thành công"))
                .andExpect(jsonPath("$.duLieu.refreshToken").doesNotExist())
                .andExpect(cookiePhien("refresh", BAY_NGAY));
    }

    @Test
    void idTokenGoogleRongHoacQuaDaiTraVe400() throws Exception {
        mockMvc.perform(post(DANG_NHAP_GOOGLE).contentType(MediaType.APPLICATION_JSON).content("{\"idToken\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("idToken")));
        mockMvc.perform(post(DANG_NHAP_GOOGLE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"" + "a".repeat(4097) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("idToken")));
        verifyNoInteractions(dangNhapGoogleService);
    }

    @Test
    void loiDangNhapGoogleTraVe401Va503() throws Exception {
        when(dangNhapGoogleService.dangNhap(any(), any()))
                .thenThrow(new LoiNghiepVu(MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE))
                .thenThrow(new LoiNghiepVu(MaLoi.DANG_NHAP_GOOGLE_KHONG_KHA_DUNG));

        mockMvc.perform(post(DANG_NHAP_GOOGLE).contentType(MediaType.APPLICATION_JSON).content("{\"idToken\": \"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("GOOGLE_TOKEN_KHONG_HOP_LE"));
        mockMvc.perform(post(DANG_NHAP_GOOGLE).contentType(MediaType.APPLICATION_JSON).content("{\"idToken\": \"x\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.maLoi").value("DANG_NHAP_GOOGLE_KHONG_KHA_DUNG"));
    }

    /** Set-Cookie của refresh token: HttpOnly, Secure, SameSite=Strict, chỉ cho /api/auth. */
    private static ResultMatcher cookiePhien(String giaTri, long maxAge) {
        return header().string(HttpHeaders.SET_COOKIE, allOf(
                startsWith(CookiePhien.TEN + "=" + giaTri + ";"),
                containsString("Path=/api/auth"),
                containsString("Max-Age=" + maxAge + ";"),
                containsString("HttpOnly"),
                containsString("Secure"),
                containsString("SameSite=Strict")));
    }

    private static DangNhapResponse phienMoi(String refreshToken) {
        return new DangNhapResponse("access", refreshToken, "Bearer", 1800, new TaiKhoanResponse(1L, "A",
                "a@example.com", null, VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT));
    }

    private ResultActions guiDatLai(String token, String matKhauMoi) throws Exception {
        String body = """
                {"token": "%s", "matKhauMoi": "%s"}
                """.formatted(token, matKhauMoi);
        return mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions guiDangNhap(String email, String matKhau) throws Exception {
        String body = """
                {"email": "%s", "matKhau": "%s"}
                """.formatted(email, matKhau);
        return mockMvc.perform(post(DANG_NHAP).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions guiDangKy(String email, String matKhau, String soDienThoai) throws Exception {
        String body = """
                {"hoTen": "Nguyễn Văn A", "email": "%s", "matKhau": "%s", "soDienThoai": "%s"}
                """.formatted(email, matKhau, soDienThoai);
        return mockMvc.perform(post(DANG_KY).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions assertLoiTruong(ResultActions ketQua, String truong) throws Exception {
        ketQua.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem(truong)));
        verifyNoInteractions(xacThucService);
        return ketQua;
    }

}
