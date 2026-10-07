package iuh.fit.se.eclinic.catalog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import iuh.fit.se.eclinic.catalog.dto.request.PhongKhamRequest;
import iuh.fit.se.eclinic.catalog.dto.response.PhongKhamQuanTriResponse;
import iuh.fit.se.eclinic.catalog.service.QuanLyPhongKhamService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Danh mục phòng khám của quản trị viên (màn hình Medical Catalog). Danh sách phòng đang hoạt động để chọn khi xếp ca
 * vẫn ở {@link PhongKhamController}.
 */
@Tag(name = "Quản trị phòng khám", description = "Danh mục phòng khám: danh sách, thêm, sửa, ngừng hoạt động / hoạt động lại (quản trị viên)")
@RestController
@RequestMapping("/api/catalog/quan-tri/phong-kham")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriPhongKhamController {

    private final QuanLyPhongKhamService quanLyPhongKhamService;

    @Operation(summary = "Mọi phòng khám theo tên (phân trang, kể cả phòng ngừng hoạt động) kèm số ca sắp tới; lọc theo từ"
            + " khoá (tên phòng, tầng), chuyên khoa, trạng thái")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<PhongKhamQuanTriResponse>> danhSach(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) Long idChuyenKhoa,
            @RequestParam(required = false) TrangThaiPhongKham trangThai,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanLyPhongKhamService.danhSach(tuKhoa, idChuyenKhoa, trangThai, trang, kichThuoc));
    }

    @Operation(summary = "Thêm phòng khám (409 TEN_PHONG_DA_TON_TAI nếu trùng tên)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<PhongKhamQuanTriResponse> them(@Valid @RequestBody PhongKhamRequest request) {
        return PhanHoiApi.ok(quanLyPhongKhamService.them(request), "Đã thêm phòng khám");
    }

    @Operation(summary = "Sửa tên phòng, tầng, chuyên khoa (đổi chuyên khoa khi phòng còn ca sắp tới: 409"
            + " PHONG_CON_CA_LAM_VIEC)")
    @PutMapping("/{id}")
    public PhanHoiApi<PhongKhamQuanTriResponse> sua(@PathVariable Long id,
            @Valid @RequestBody PhongKhamRequest request) {
        return PhanHoiApi.ok(quanLyPhongKhamService.sua(id, request), "Đã cập nhật phòng khám");
    }

    @Operation(summary = "Cho phòng ngừng hoạt động: không xếp ca mới vào phòng được nữa (409 PHONG_CON_CA_LAM_VIEC nếu"
            + " phòng còn ca sắp tới, chuyển các ca đó sang phòng khác trước)")
    @PostMapping("/{id}/ngung-hoat-dong")
    public PhanHoiApi<PhongKhamQuanTriResponse> ngungHoatDong(@PathVariable Long id) {
        return PhanHoiApi.ok(quanLyPhongKhamService.ngungHoatDong(id), "Phòng khám đã ngừng hoạt động");
    }

    @Operation(summary = "Cho phòng hoạt động lại")
    @PostMapping("/{id}/hoat-dong-lai")
    public PhanHoiApi<PhongKhamQuanTriResponse> hoatDongLai(@PathVariable Long id) {
        return PhanHoiApi.ok(quanLyPhongKhamService.hoatDongLai(id), "Phòng khám đã hoạt động lại");
    }
}
