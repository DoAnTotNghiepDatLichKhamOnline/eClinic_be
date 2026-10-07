package iuh.fit.se.eclinic.medical.controller;

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
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import iuh.fit.se.eclinic.medical.dto.request.ThuocRequest;
import iuh.fit.se.eclinic.medical.dto.response.ThuocQuanTriResponse;
import iuh.fit.se.eclinic.medical.service.QuanLyThuocService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Danh mục thuốc của quản trị viên (màn hình Medical Catalog): xác minh thuốc bác sĩ tự thêm khi kê đơn, thêm / sửa, cho
 * ngừng dùng. Gợi ý thuốc cho bác sĩ ở {@link KhamBenhController}.
 */
@Tag(name = "Quản trị danh mục thuốc", description = "Danh sách, thêm, sửa, xác minh, ngừng dùng / dùng lại (quản trị viên)")
@RestController
@RequestMapping("/api/medical/quan-tri/thuoc")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriThuocController {

    private final QuanLyThuocService quanLyThuocService;

    @Operation(summary = "Danh mục thuốc (phân trang, thuốc chưa xác minh đứng trước rồi theo tên) kèm số lần đã kê; lọc"
            + " theo tên, đã xác minh, trạng thái")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<ThuocQuanTriResponse>> danhSach(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) Boolean daXacMinh,
            @RequestParam(required = false) TrangThaiThuoc trangThai,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanLyThuocService.danhSach(tuKhoa, daXacMinh, trangThai, trang, kichThuoc));
    }

    @Operation(summary = "Thêm thuốc đã xác minh (409 TEN_THUOC_DA_TON_TAI nếu trùng tên, không phân biệt hoa thường /"
            + " khoảng trắng)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<ThuocQuanTriResponse> them(@Valid @RequestBody ThuocRequest request) {
        return PhanHoiApi.ok(quanLyThuocService.them(request), "Đã thêm thuốc vào danh mục");
    }

    @Operation(summary = "Sửa tên, đơn vị, mô tả (thuốc đã có trong đơn thuốc mà đổi sang tên khác: 409 THUOC_DA_DUOC_KE)")
    @PutMapping("/{id}")
    public PhanHoiApi<ThuocQuanTriResponse> sua(@PathVariable Long id, @Valid @RequestBody ThuocRequest request) {
        return PhanHoiApi.ok(quanLyThuocService.sua(id, request), "Đã cập nhật thuốc");
    }

    @Operation(summary = "Xác minh thuốc do bác sĩ thêm khi kê đơn")
    @PostMapping("/{id}/xac-minh")
    public PhanHoiApi<ThuocQuanTriResponse> xacMinh(@PathVariable Long id) {
        return PhanHoiApi.ok(quanLyThuocService.xacMinh(id), "Đã xác minh thuốc");
    }

    @Operation(summary = "Ngừng dùng: thuốc không còn được gợi ý và không kê mới được; đơn thuốc cũ vẫn hiển thị")
    @PostMapping("/{id}/ngung-dung")
    public PhanHoiApi<ThuocQuanTriResponse> ngungDung(@PathVariable Long id) {
        return PhanHoiApi.ok(quanLyThuocService.ngungDung(id), "Thuốc đã ngừng dùng");
    }

    @Operation(summary = "Dùng lại thuốc đã ngừng dùng")
    @PostMapping("/{id}/dung-lai")
    public PhanHoiApi<ThuocQuanTriResponse> dungLai(@PathVariable Long id) {
        return PhanHoiApi.ok(quanLyThuocService.dungLai(id), "Thuốc đã được dùng lại");
    }
}
