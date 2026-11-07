package iuh.fit.se.eclinic.identity;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
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

import iuh.fit.se.eclinic.common.enums.NgonNgu;
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
import iuh.fit.se.eclinic.identity.service.DoiEmailService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.XacThucService;
import jakarta.servlet.http.Cookie;

/** Tầng web của /api/auth: công khai, validate dữ liệu vào, định dạng JSON, cookie refresh token. */
@WebMvcTest(XacThucController.class)
@AutoConfigureJson
@Import({ BaoMatConfig.class, CookiePhien.class })
// @WebMvcTest không quét @ConfigurationProperties: CookiePhien cần 2 record này
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
    @MockitoBean DoiEmailService doiEmailService;

    @Test
    void dangKyKhongCanDangNhapTraVe201() throws Exception {
        when(xacThucService.dangKy(any(DangKyRequest.class))).thenReturn(new TaiKhoanResponse(7L, "Nguyễn Văn A",
                "a@example.com", "0912345678", VaiTro.BENH_NHAN, TrangThaiTaiKhoan.CHO_XAC_NHAN, NgonNgu.VI));

        guiDangKy("a@example.com", "123456", "0912345678")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu.id").value(7))
                .andExpect(jsonPath("$.duLieu.trangThai").value("CHO_XAC_NHAN"))
                .andExpect(jsonPath("$.duLieu.matKhauHash").doesNotExist())
                .andExpect(jsonPath("$.thongDiep").value("Đăng ký thành công, vui lòng kiểm tra email để kích hoạt tài khoản"));
    }

    @Test
    void matKhauQua72ByteTraVe400ChuKhongPhai500() throws Exception {
        // 25 ký tự nhưng 75 byte UTF-8: qua được @Size theo ký tự nhưng BCrypt sẽ ném lỗi
        assertLoiTruong(guiDangKy("a@example.com", "ệ".repeat(25), "0912345678"), "matKhau")
                .andExpect(jsonPath("$.chiTiet[0].thongDiep").value("Mật khẩu quá dài (tối đa 72 byte)"));
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
                "a@example.com", null, VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, NgonNgu.VI));
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
