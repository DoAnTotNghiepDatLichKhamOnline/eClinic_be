package iuh.fit.se.eclinic.identity.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatHoSoRequest;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;
import iuh.fit.se.eclinic.identity.dto.request.DoiNgonNguRequest;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.service.HoSoCaNhanService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Hồ sơ cá nhân của người đang đăng nhập và đổi mật khẩu. */
@Tag(name = "Hồ sơ cá nhân", description = "Xem, cập nhật hồ sơ và đổi mật khẩu của người đang đăng nhập")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class HoSoCaNhanController {

    private final HoSoCaNhanService hoSoCaNhanService;
    private final MatKhauService matKhauService;

    @Operation(summary = "Xem hồ sơ cá nhân: tài khoản + hồ sơ bác sĩ / hồ sơ bệnh nhân (chỉ xem)")
    @GetMapping
    public PhanHoiApi<HoSoCaNhanResponse> layHoSo() {
        return PhanHoiApi.ok(hoSoCaNhanService.layHoSo(NguoiDungHienTai.layIdTaiKhoan()));
    }

    @Operation(summary = "Cập nhật họ tên, số điện thoại (bác sĩ chỉ đổi được số điện thoại)")
    @PutMapping
    public PhanHoiApi<HoSoCaNhanResponse> capNhat(@Valid @RequestBody CapNhatHoSoRequest request) {
        return PhanHoiApi.ok(hoSoCaNhanService.capNhat(NguoiDungHienTai.layIdTaiKhoan(), request),
                "thanhCong.capNhatHoSoCaNhan");
    }

    @Operation(summary = "Lưu ngôn ngữ ưa dùng (VI / EN) của tài khoản, dùng cho email và nhắc lịch; thông điệp của API vẫn theo"
            + " header Accept-Language của từng request")
    @PutMapping("/language")
    public PhanHoiApi<HoSoCaNhanResponse> doiNgonNgu(@Valid @RequestBody DoiNgonNguRequest request) {
        return PhanHoiApi.ok(hoSoCaNhanService.doiNgonNgu(NguoiDungHienTai.layIdTaiKhoan(), request.ngonNgu()),
                "thanhCong.doiNgonNgu");
    }

    @Operation(summary = "Đổi mật khẩu (phải nhập mật khẩu hiện tại); các thiết bị khác bị đăng xuất, thiết bị này giữ nguyên")
    @PutMapping("/change-password")
    public PhanHoiApi<Void> doiMatKhau(@Valid @RequestBody DoiMatKhauRequest request) {
        matKhauService.doiMatKhau(NguoiDungHienTai.layIdTaiKhoan(), NguoiDungHienTai.layMaPhien(), request);
        return PhanHoiApi.ok(null, "thanhCong.doiMatKhau");
    }

}
