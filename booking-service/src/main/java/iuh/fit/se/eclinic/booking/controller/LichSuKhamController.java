package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.PhamViLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LanKhamCuaToiResponse;
import iuh.fit.se.eclinic.booking.service.LichSuKhamService;
import iuh.fit.se.eclinic.booking.service.LienKetHoSoService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Lịch sử khám của bệnh nhân đã đăng nhập: các lượt đã khám xong kèm kết quả. Luôn thao tác trên tài khoản của chính
 * người gọi.
 */
@Tag(name = "Lịch sử khám", description = "Các lượt đã khám kèm chẩn đoán, lời dặn, đơn thuốc của bệnh nhân")
@RestController
@RequestMapping("/api/booking/lich-su-kham/cua-toi")
@PreAuthorize("hasRole('BENH_NHAN')")
@RequiredArgsConstructor
public class LichSuKhamController {

    private final LichSuKhamService lichSuKhamService;
    private final LienKetHoSoService lienKetHoSoService;

    @Operation(summary = "Các lượt đã khám mà tôi được xem kết quả (tôi là người khám, hoặc tôi đã đặt lịch), mới nhất"
            + " trước; lọc của tôi / của người khác (cuaAi), phân trang")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<LanKhamCuaToiResponse>> cuaToi(
            @RequestParam(defaultValue = "TAT_CA") PhamViLichHen cuaAi,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        Long idTaiKhoan = NguoiDungHienTai.layIdTaiKhoan();
        // Lượt khám của hồ sơ đặt như khách bằng số CCCD đã khai khi đăng ký phải có ngay lần xem đầu tiên (quy tắc #3)
        lienKetHoSoService.thuLienKet(idTaiKhoan);
        return PhanHoiApi.ok(lichSuKhamService.cuaToi(idTaiKhoan, cuaAi, trang, kichThuoc));
    }

}
