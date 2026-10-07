package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaBacSiResponse;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanDanhGiaResponse;
import iuh.fit.se.eclinic.booking.service.DanhGiaService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** Bác sĩ xem đánh giá của CHÍNH MÌNH. Ẩn danh: không có tên bệnh nhân, không có mã lịch hẹn. */
@Tag(name = "Đánh giá của bác sĩ", description = "Bác sĩ xem điểm trung bình và các đánh giá mình nhận được")
@RestController
@RequestMapping("/api/booking/bac-si/toi/danh-gia")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class DanhGiaCuaBacSiController {

    private final DanhGiaService danhGiaService;

    @Operation(summary = "Điểm trung bình (1 chữ số thập phân, null khi chưa có), số đánh giá và phân bố theo sao của tôi")
    @GetMapping("/tong-quan")
    public PhanHoiApi<TongQuanDanhGiaResponse> tongQuan() {
        return PhanHoiApi.ok(danhGiaService.tongQuanCuaBacSi(NguoiDungHienTai.layIdTaiKhoan()));
    }

    @Operation(summary = "Các đánh giá tôi nhận được, mới nhất trước, phân trang; không có tên bệnh nhân")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<DanhGiaCuaBacSiResponse>> danhSach(
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(danhGiaService.cuaBacSi(NguoiDungHienTai.layIdTaiKhoan(), trang, kichThuoc));
    }
}
