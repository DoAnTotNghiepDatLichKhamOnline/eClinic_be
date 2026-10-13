package iuh.fit.se.eclinic.catalog.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaQuanTriResponse;
import iuh.fit.se.eclinic.catalog.service.ChuyenKhoaService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Danh mục chuyên khoa của quản trị viên: chữ đã lưu của cả 2 ngôn ngữ, để đổ vào form sửa. Thêm / sửa / xoá ở
 * {@link ChuyenKhoaController}.
 */
@Tag(name = "Quản trị chuyên khoa", description = "Danh mục chuyên khoa kèm bản tiếng Anh (quản trị viên)")
@RestController
@RequestMapping("/api/catalog/quan-tri/chuyen-khoa")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriChuyenKhoaController {

    private final ChuyenKhoaService chuyenKhoaService;

    @Operation(summary = "Danh mục chuyên khoa (phân trang, theo tên tiếng Việt); từ khoá so với tên của cả 2 ngôn ngữ")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<ChuyenKhoaQuanTriResponse>> danhSach(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(chuyenKhoaService.danhSachQuanTri(tuKhoa, trang, kichThuoc));
    }

}
