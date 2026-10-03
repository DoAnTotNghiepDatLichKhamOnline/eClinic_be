package iuh.fit.se.eclinic.catalog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.catalog.service.ChuyenKhoaService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * ADM-01: quản lý chuyên khoa. Xem: công khai (app.bao-mat.duong-dan-cong-khai); thêm/sửa/xoá: chỉ quản trị viên.
 * <p>
 * Controller mẫu: chỉ kiểm tra dữ liệu vào, gọi service, bọc kết quả trong PhanHoiApi. Lỗi do XuLyLoiHandler xử lý.
 */
@Tag(name = "Chuyên khoa", description = "ADM-01: quản lý chuyên khoa")
@RestController
@RequestMapping("/api/catalog/chuyen-khoa")
@RequiredArgsConstructor
public class ChuyenKhoaController {

    private final ChuyenKhoaService chuyenKhoaService;

    @Operation(summary = "Tìm kiếm chuyên khoa theo tên (phân trang, sắp xếp theo tên)")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<ChuyenKhoaResponse>> timKiem(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(chuyenKhoaService.timKiem(tuKhoa, trang, kichThuoc));
    }

    @Operation(summary = "Xem chi tiết chuyên khoa")
    @GetMapping("/{id}")
    public PhanHoiApi<ChuyenKhoaResponse> layTheoId(@PathVariable Long id) {
        return PhanHoiApi.ok(chuyenKhoaService.layTheoId(id));
    }

    @Operation(summary = "Thêm chuyên khoa (quản trị viên)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public PhanHoiApi<ChuyenKhoaResponse> tao(@Valid @RequestBody ChuyenKhoaRequest request) {
        return PhanHoiApi.ok(chuyenKhoaService.tao(request), "Đã thêm chuyên khoa");
    }

    @Operation(summary = "Sửa chuyên khoa (quản trị viên)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public PhanHoiApi<ChuyenKhoaResponse> capNhat(@PathVariable Long id, @Valid @RequestBody ChuyenKhoaRequest request) {
        return PhanHoiApi.ok(chuyenKhoaService.capNhat(id, request), "Đã cập nhật chuyên khoa");
    }

    @Operation(summary = "Xoá chuyên khoa (quản trị viên) — chỉ khi không còn phòng khám / bác sĩ")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public PhanHoiApi<Void> xoa(@PathVariable Long id) {
        chuyenKhoaService.xoa(id);
        return PhanHoiApi.ok(null, "Đã xoá chuyên khoa");
    }

}
