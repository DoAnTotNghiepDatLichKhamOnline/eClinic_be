package iuh.fit.se.eclinic.identity.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import iuh.fit.se.eclinic.identity.dto.request.DoiEmailRequest;
import iuh.fit.se.eclinic.identity.dto.response.YeuCauDoiEmailResponse;
import iuh.fit.se.eclinic.identity.service.DoiEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Đổi email đăng nhập của người đang đăng nhập: gửi yêu cầu, xem yêu cầu đang chờ, huỷ yêu cầu. Mọi vai trò đều gọi được.
 * Email chỉ đổi khi token trong liên kết gửi tới địa chỉ mới được gửi về POST /api/auth/confirm-email-change
 * (XacThucController; không cần đăng nhập).
 */
@Tag(name = "Đổi email", description = "Yêu cầu, xem và huỷ việc đổi email đăng nhập của người đang đăng nhập")
@RestController
@RequestMapping("/api/users/me/change-email")
@RequiredArgsConstructor
public class DoiEmailController {

    private final DoiEmailService doiEmailService;

    @Operation(summary = "Yêu cầu đổi email (phải nhập mật khẩu hiện tại): gửi liên kết xác nhận tới email mới, báo cho email đang dùng")
    @PostMapping
    public PhanHoiApi<YeuCauDoiEmailResponse> yeuCau(@Valid @RequestBody DoiEmailRequest request) {
        return PhanHoiApi.ok(doiEmailService.yeuCau(NguoiDungHienTai.layIdTaiKhoan(), request),
                "Đã gửi liên kết xác nhận tới email mới, vui lòng kiểm tra hộp thư");
    }

    @Operation(summary = "Yêu cầu đổi email đang chờ xác nhận; không có thì duLieu vắng mặt")
    @GetMapping
    public PhanHoiApi<YeuCauDoiEmailResponse> layYeuCauDangCho() {
        return PhanHoiApi.ok(doiEmailService.layYeuCauDangCho(NguoiDungHienTai.layIdTaiKhoan()));
    }

    @Operation(summary = "Huỷ yêu cầu đổi email đang chờ (liên kết đã gửi hết hiệu lực); luôn trả thành công")
    @DeleteMapping
    public PhanHoiApi<Void> huy() {
        doiEmailService.huy(NguoiDungHienTai.layIdTaiKhoan());
        return PhanHoiApi.ok(null, "Đã huỷ yêu cầu đổi email");
    }

}
