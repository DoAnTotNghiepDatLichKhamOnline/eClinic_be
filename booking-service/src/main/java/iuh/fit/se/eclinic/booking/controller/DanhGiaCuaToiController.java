package iuh.fit.se.eclinic.booking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.DanhGiaRequest;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaToiResponse;
import iuh.fit.se.eclinic.booking.service.DanhGiaService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Bệnh nhân đánh giá 1 lượt khám đã hoàn thành của mình (hoặc lượt mình đã đặt cho người khác). Đánh giá đã gửi trả về
 * trong chi tiết lịch hẹn: GET /api/booking/lich-hen/cua-toi/{maPhieuKham} (trường {@code danhGia}, {@code duocDanhGia}).
 */
@Tag(name = "Đánh giá lượt khám", description = "Bệnh nhân gửi / sửa đánh giá cho lượt khám đã hoàn thành")
@RestController
@RequestMapping("/api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia")
@PreAuthorize("hasRole('BENH_NHAN')")
@RequiredArgsConstructor
public class DanhGiaCuaToiController {

    private final DanhGiaService danhGiaService;

    @Operation(summary = "Gửi đánh giá (1..5 sao, nhận xét không bắt buộc) cho lượt khám đã hoàn thành; mỗi lịch hẹn 1 lần"
            + " (409 LICH_HEN_CHUA_KHAM_XONG, 409 DA_DANH_GIA)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<DanhGiaCuaToiResponse> gui(@PathVariable String maPhieuKham,
            @Valid @RequestBody DanhGiaRequest request) {
        return PhanHoiApi.ok(danhGiaService.gui(NguoiDungHienTai.layIdTaiKhoan(), maPhieuKham, request),
                "Đã gửi đánh giá");
    }

    @Operation(summary = "Sửa đánh giá đã gửi trong thời hạn sửa (mặc định 7 ngày; quá hạn: 409 HET_HAN_SUA_DANH_GIA)")
    @PutMapping
    public PhanHoiApi<DanhGiaCuaToiResponse> sua(@PathVariable String maPhieuKham,
            @Valid @RequestBody DanhGiaRequest request) {
        return PhanHoiApi.ok(danhGiaService.sua(NguoiDungHienTai.layIdTaiKhoan(), maPhieuKham, request),
                "Đã cập nhật đánh giá");
    }
}
