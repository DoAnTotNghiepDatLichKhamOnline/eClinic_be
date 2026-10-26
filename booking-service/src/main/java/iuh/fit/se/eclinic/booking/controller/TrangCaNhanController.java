package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.TrangCaNhanResponse;
import iuh.fit.se.eclinic.booking.service.LienKetHoSoService;
import iuh.fit.se.eclinic.booking.service.TrangCaNhanService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import lombok.RequiredArgsConstructor;

/**
 * Trang cá nhân của bệnh nhân đã đăng nhập. Luôn thao tác trên tài khoản của chính người gọi.
 */
@Tag(name = "Trang cá nhân", description = "Hồ sơ, người thân đã lưu và lịch sắp tới của bệnh nhân trong 1 lần gọi")
@RestController
@RequestMapping("/api/booking/trang-ca-nhan/cua-toi")
@PreAuthorize("hasRole('BENH_NHAN')")
@RequiredArgsConstructor
public class TrangCaNhanController {

    private final TrangCaNhanService trangCaNhanService;
    private final LienKetHoSoService lienKetHoSoService;

    @Operation(summary = "Hồ sơ của tôi, người thân đã lưu, lịch sắp tới của tôi / của người khác và số lịch mỗi loại")
    @GetMapping
    public PhanHoiApi<TrangCaNhanResponse> cuaToi() {
        Long idTaiKhoan = NguoiDungHienTai.layIdTaiKhoan();
        // Lịch đặt như khách bằng số CCCD đã khai khi đăng ký phải có ngay lần xem đầu tiên (quy tắc #3)
        lienKetHoSoService.thuLienKet(idTaiKhoan);
        return PhanHoiApi.ok(trangCaNhanService.cuaToi(idTaiKhoan));
    }

}
