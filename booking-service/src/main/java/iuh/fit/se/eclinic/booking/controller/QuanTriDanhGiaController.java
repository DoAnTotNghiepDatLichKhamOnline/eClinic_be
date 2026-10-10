package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaQuanTriResponse;
import iuh.fit.se.eclinic.booking.service.DanhGiaService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** Quản trị viên đọc đánh giá của bệnh nhân, kèm tên bệnh nhân và bác sĩ. Chỉ đọc. */
@Tag(name = "Quản trị đánh giá", description = "Quản trị viên xem đánh giá của bệnh nhân (kèm tên bệnh nhân, bác sĩ)")
@RestController
@RequestMapping("/api/booking/quan-tri/danh-gia")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriDanhGiaController {

    private final DanhGiaService danhGiaService;

    @Operation(summary = "Đánh giá của bệnh nhân, mới nhất trước, phân trang; lọc theo bác sĩ và theo mức sao tối đa"
            + " (soSaoToiDa=2: chỉ đánh giá 1-2 sao)")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<DanhGiaQuanTriResponse>> danhSach(
            @RequestParam(required = false) Long idBacSi,
            @RequestParam(required = false) @Min(1) @Max(5) Integer soSaoToiDa,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(danhGiaService.choQuanTri(idBacSi, soSaoToiDa, trang, kichThuoc));
    }
}
