package iuh.fit.se.eclinic.identity.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapGoogleRequest;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.request.DatLaiMatKhauRequest;
import iuh.fit.se.eclinic.identity.dto.request.DatMatKhauLanDauRequest;
import iuh.fit.se.eclinic.identity.dto.request.GuiLaiXacThucRequest;
import iuh.fit.se.eclinic.identity.dto.request.PhienDangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.request.QuenMatKhauRequest;
import iuh.fit.se.eclinic.identity.dto.request.XacThucEmailRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanResponse;
import iuh.fit.se.eclinic.identity.service.DangNhapGoogleService;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.DoiEmailService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.XacThucService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AUTH-01: đăng ký và kích hoạt tài khoản qua liên kết email. AUTH-02: đăng nhập, làm mới phiên, đăng xuất.
 * AUTH-03: quên / đặt lại mật khẩu qua liên kết email. Đăng nhập bằng Google (chỉ bệnh nhân). Xác nhận đổi email
 * (yêu cầu đổi nằm ở DoiEmailController).
 * Mọi đường dẫn ở đây đều công khai (app.bao-mat.duong-dan-cong-khai); đăng xuất chỉ cần refresh token.
 * Đường dẫn theo tài liệu API: /api/auth/...
 * <p>
 * Refresh token nằm trong cookie HttpOnly {@value CookiePhien#TEN} (không có trong body). Làm mới phiên / đăng xuất
 * đọc cookie trước, không có thì đọc {@code refreshToken} trong body (để thử bằng Swagger, curl).
 */
@Tag(name = "Xác thực", description = "AUTH-01: đăng ký, kích hoạt tài khoản. AUTH-02: đăng nhập, làm mới phiên, đăng xuất."
        + " AUTH-03: quên / đặt lại mật khẩu. Đăng nhập bằng Google. Xác nhận đổi email")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class XacThucController {

    private final XacThucService xacThucService;
    private final DangNhapService dangNhapService;
    private final MatKhauService matKhauService;
    private final DangNhapGoogleService dangNhapGoogleService;
    private final DoiEmailService doiEmailService;
    private final CookiePhien cookiePhien;

    @Operation(summary = "Đăng ký tài khoản bệnh nhân, gửi liên kết kích hoạt qua email")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<TaiKhoanResponse> dangKy(@Valid @RequestBody DangKyRequest request) {
        return PhanHoiApi.ok(xacThucService.dangKy(request),
                "Đăng ký thành công, vui lòng kiểm tra email để kích hoạt tài khoản");
    }

    @Operation(summary = "Kích hoạt tài khoản bằng token trong liên kết email")
    @PostMapping("/verify-email")
    public PhanHoiApi<Void> xacThucEmail(@Valid @RequestBody XacThucEmailRequest request) {
        xacThucService.xacThucEmail(request.token());
        return PhanHoiApi.ok(null, "Xác thực email thành công");
    }

    @Operation(summary = "Gửi lại liên kết kích hoạt (luôn trả cùng 1 thông báo)")
    @PostMapping("/resend-verification")
    public PhanHoiApi<Void> guiLaiXacThuc(@Valid @RequestBody GuiLaiXacThucRequest request) {
        xacThucService.guiLaiXacThuc(request.email());
        return PhanHoiApi.ok(null, "Nếu email đã đăng ký và chưa kích hoạt, liên kết mới đã được gửi");
    }

    @Operation(summary = "Đăng nhập bằng email và mật khẩu: access token trong body, refresh token trong cookie")
    @PostMapping("/login")
    public ResponseEntity<PhanHoiApi<DangNhapResponse>> dangNhap(@Valid @RequestBody DangNhapRequest request,
            @RequestHeader(value = "User-Agent", required = false) String thongTinThietBi) {
        return capCookie(dangNhapService.dangNhap(request, thongTinThietBi), "Đăng nhập thành công");
    }

    @Operation(summary = "Lần đăng nhập đầu của tài khoản bác sĩ mới (đăng nhập trả 403 PHAI_DOI_MAT_KHAU): đặt mật khẩu của"
            + " mình bằng email + mật khẩu mặc định; sau đó đăng nhập lại bằng mật khẩu mới")
    @PostMapping("/first-password")
    public PhanHoiApi<Void> datMatKhauLanDau(@Valid @RequestBody DatMatKhauLanDauRequest request) {
        dangNhapService.datMatKhauLanDau(request);
        return PhanHoiApi.ok(null, "Đã đặt mật khẩu, vui lòng đăng nhập bằng mật khẩu mới");
    }

    @Operation(summary = "Đổi refresh token (cookie, hoặc body khi thử API) lấy cặp token mới; token cũ hết hiệu lực")
    @PostMapping("/refresh-token")
    public ResponseEntity<PhanHoiApi<DangNhapResponse>> lamMoi(
            @Parameter(hidden = true) @CookieValue(name = CookiePhien.TEN, required = false) String cookie,
            @Valid @RequestBody(required = false) PhienDangNhapRequest request) {
        String refreshToken = chonRefreshToken(cookie, request);
        if (refreshToken == null) {
            throw new LoiNghiepVu(MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        }
        return capCookie(dangNhapService.lamMoi(refreshToken), "Làm mới phiên đăng nhập thành công");
    }

    @Operation(summary = "Đăng xuất phiên của refresh token (cookie hoặc body) và xoá cookie; luôn trả thành công")
    @PostMapping("/logout")
    public ResponseEntity<PhanHoiApi<Void>> dangXuat(
            @Parameter(hidden = true) @CookieValue(name = CookiePhien.TEN, required = false) String cookie,
            @Valid @RequestBody(required = false) PhienDangNhapRequest request) {
        String refreshToken = chonRefreshToken(cookie, request);
        if (refreshToken != null) {
            dangNhapService.dangXuat(refreshToken);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookiePhien.xoa())
                .body(PhanHoiApi.ok(null, "Đã đăng xuất"));
    }

    @Operation(summary = "Gửi liên kết đặt lại mật khẩu qua email (luôn trả cùng 1 thông báo)")
    @PostMapping("/forgot-password")
    public PhanHoiApi<Void> quenMatKhau(@Valid @RequestBody QuenMatKhauRequest request) {
        matKhauService.quenMatKhau(request.email());
        return PhanHoiApi.ok(null, "Nếu email đã đăng ký, liên kết đặt lại mật khẩu đã được gửi");
    }

    @Operation(summary = "Đặt mật khẩu mới bằng token trong liên kết email; mọi thiết bị bị đăng xuất")
    @PostMapping("/reset-password")
    public PhanHoiApi<Void> datLaiMatKhau(@Valid @RequestBody DatLaiMatKhauRequest request) {
        matKhauService.datLaiMatKhau(request.token(), request.matKhauMoi());
        return PhanHoiApi.ok(null, "Đặt lại mật khẩu thành công, vui lòng đăng nhập lại");
    }

    // Không xoá cookie refresh token: trình duyệt mở liên kết có thể đang đăng nhập 1 tài khoản khác
    @Operation(summary = "Xác nhận đổi email bằng token trong liên kết gửi tới email mới; mọi thiết bị bị đăng xuất")
    @PostMapping("/confirm-email-change")
    public PhanHoiApi<Void> xacNhanDoiEmail(@Valid @RequestBody XacThucEmailRequest request) {
        doiEmailService.xacNhan(request.token());
        return PhanHoiApi.ok(null, "Đã đổi email đăng nhập, vui lòng đăng nhập lại bằng email mới");
    }

    @Operation(summary = "Đăng nhập bằng ID token Google (bệnh nhân): access token trong body, refresh token trong cookie")
    @PostMapping("/google")
    public ResponseEntity<PhanHoiApi<DangNhapResponse>> dangNhapGoogle(@Valid @RequestBody DangNhapGoogleRequest request,
            @RequestHeader(value = "User-Agent", required = false) String thongTinThietBi) {
        return capCookie(dangNhapGoogleService.dangNhap(request.idToken(), thongTinThietBi),
                "Đăng nhập Google thành công");
    }

    /** refreshToken không được đưa ra JSON (@JsonIgnore), chỉ đi trong cookie. */
    private ResponseEntity<PhanHoiApi<DangNhapResponse>> capCookie(DangNhapResponse ketQua, String thongDiep) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookiePhien.tao(ketQua.refreshToken()))
                .body(PhanHoiApi.ok(ketQua, thongDiep));
    }

    /** Cookie trước (trình duyệt), rồi tới body (Swagger, curl). Không có thì null. */
    private static String chonRefreshToken(String cookie, PhienDangNhapRequest request) {
        if (StringUtils.hasText(cookie)) {
            return cookie;
        }
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            return request.refreshToken();
        }
        return null;
    }

}
