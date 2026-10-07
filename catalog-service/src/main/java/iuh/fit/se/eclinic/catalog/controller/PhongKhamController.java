package iuh.fit.se.eclinic.catalog.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.catalog.dto.response.PhongKhamResponse;
import iuh.fit.se.eclinic.catalog.service.PhongKhamService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import lombok.RequiredArgsConstructor;

/**
 * Danh sách phòng khám đang hoạt động, chỉ đọc: quản trị viên chọn phòng khi xếp ca, bác sĩ chọn phòng mong muốn khi xin
 * đổi ca. Thêm / sửa / ẩn phòng khám chưa có API.
 */
@Tag(name = "Phòng khám", description = "Danh sách phòng khám đang hoạt động (chỉ đọc)")
@RestController
@RequestMapping("/api/catalog/phong-kham")
@PreAuthorize("hasAnyRole('QUAN_TRI_VIEN', 'BAC_SI')")
@RequiredArgsConstructor
public class PhongKhamController {

    private final PhongKhamService phongKhamService;

    @Operation(summary = "Các phòng khám đang hoạt động theo tên phòng; idChuyenKhoa để chỉ lấy phòng của 1 chuyên khoa")
    @GetMapping
    public PhanHoiApi<List<PhongKhamResponse>> danhSach(@RequestParam(required = false) Long idChuyenKhoa) {
        return PhanHoiApi.ok(phongKhamService.danhSachDangHoatDong(idChuyenKhoa));
    }

}
