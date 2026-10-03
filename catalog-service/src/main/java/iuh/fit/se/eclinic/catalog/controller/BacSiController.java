package iuh.fit.se.eclinic.catalog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.catalog.service.BacSiService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * DOC-03: tìm bác sĩ theo chuyên khoa / tên khi đặt lịch. Công khai (app.bao-mat.duong-dan-cong-khai).
 * Chỉ trả về bác sĩ đang công tác có tài khoản đã kích hoạt.
 */
@Tag(name = "Bác sĩ", description = "DOC-03: tìm bác sĩ theo chuyên khoa, tên")
@RestController
@RequestMapping("/api/catalog/bac-si")
@RequiredArgsConstructor
public class BacSiController {

    private final BacSiService bacSiService;

    @Operation(summary = "Tìm bác sĩ theo chuyên khoa và tên (phân trang, sắp xếp theo tên)")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<BacSiResponse>> timKiem(
            @RequestParam(required = false) Long idChuyenKhoa,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(bacSiService.timKiem(idChuyenKhoa, tuKhoa, trang, kichThuoc));
    }

    @Operation(summary = "Xem chi tiết bác sĩ")
    @GetMapping("/{id}")
    public PhanHoiApi<BacSiChiTietResponse> layChiTiet(@PathVariable Long id) {
        return PhanHoiApi.ok(bacSiService.layChiTiet(id));
    }

}
