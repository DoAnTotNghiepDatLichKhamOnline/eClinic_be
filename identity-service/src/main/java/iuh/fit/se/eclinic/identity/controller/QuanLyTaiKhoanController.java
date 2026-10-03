package iuh.fit.se.eclinic.identity.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatTrangThaiTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.response.ChiTietTaiKhoanResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanQuanTriResponse;
import iuh.fit.se.eclinic.identity.service.QuanLyTaiKhoanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Quản trị viên quản lý tài khoản người dùng (UC-USER-01, ADM-03). Chỉ vai trò QUAN_TRI_VIEN gọi được; id của quản trị
 * viên lấy từ JWT và được service kiểm tra lại ở mỗi lời gọi.
 * Đường dẫn theo tài liệu API: /api/users, /api/users/{id} (các đường dẫn /api/users/me... thuộc controller khác).
 */
@Tag(name = "Quản lý tài khoản", description = "UC-USER-01: quản trị viên xem, vô hiệu hoá / kích hoạt lại, xoá tài khoản")
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanLyTaiKhoanController {

    private final QuanLyTaiKhoanService quanLyTaiKhoanService;

    @Operation(summary = "Danh sách tài khoản (phân trang, mới tạo đứng trước); lọc theo từ khoá (họ tên, email, số điện thoại), vai trò, trạng thái")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<TaiKhoanQuanTriResponse>> timKiem(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) VaiTro vaiTro,
            @RequestParam(required = false) TrangThaiTaiKhoan trangThai,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanLyTaiKhoanService.timKiem(NguoiDungHienTai.layIdTaiKhoan(), tuKhoa, vaiTro, trangThai,
                trang, kichThuoc));
    }

    @Operation(summary = "Xem chi tiết 1 tài khoản (kèm hồ sơ bác sĩ nếu là bác sĩ; không có dữ liệu y tế)")
    @GetMapping("/{id}")
    public PhanHoiApi<ChiTietTaiKhoanResponse> layChiTiet(@PathVariable Long id) {
        return PhanHoiApi.ok(quanLyTaiKhoanService.layChiTiet(NguoiDungHienTai.layIdTaiKhoan(), id));
    }

    @Operation(summary = "Vô hiệu hoá (VO_HIEU_HOA, bắt buộc có lý do; đăng xuất mọi thiết bị) hoặc kích hoạt lại (DA_KICH_HOAT) tài khoản; gửi email báo cho chủ tài khoản")
    @PutMapping("/{id}/status")
    public PhanHoiApi<ChiTietTaiKhoanResponse> capNhatTrangThai(@PathVariable Long id,
            @Valid @RequestBody CapNhatTrangThaiTaiKhoanRequest request) {
        ChiTietTaiKhoanResponse ketQua = quanLyTaiKhoanService.capNhatTrangThai(NguoiDungHienTai.layIdTaiKhoan(), id,
                request);
        return PhanHoiApi.ok(ketQua, request.trangThai() == TrangThaiTaiKhoan.VO_HIEU_HOA ? "Đã vô hiệu hoá tài khoản"
                : "Đã kích hoạt lại tài khoản");
    }

    @Operation(summary = "Xoá tài khoản — chỉ khi chưa có dữ liệu nào tham chiếu (hồ sơ, thông báo, phiên chat); nếu có thì 409, hãy vô hiệu hoá")
    @DeleteMapping("/{id}")
    public PhanHoiApi<Void> xoa(@PathVariable Long id) {
        quanLyTaiKhoanService.xoa(NguoiDungHienTai.layIdTaiKhoan(), id);
        return PhanHoiApi.ok(null, "Đã xoá tài khoản");
    }

}
