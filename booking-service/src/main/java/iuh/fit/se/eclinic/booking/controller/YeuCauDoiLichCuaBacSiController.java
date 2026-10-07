package iuh.fit.se.eclinic.booking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.GuiYeuCauDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.YeuCauDoiLichResponse;
import iuh.fit.se.eclinic.booking.service.YeuCauDoiLichService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * SCHED-02: bác sĩ xin đổi ca / xin nghỉ 1 ca của CHÍNH MÌNH, xem các yêu cầu đã gửi, rút yêu cầu còn chờ duyệt. Bác sĩ lấy
 * từ tài khoản đang đăng nhập; ca và yêu cầu của bác sĩ khác coi như không có (404).
 */
@Tag(name = "Yêu cầu đổi lịch của bác sĩ", description = "SCHED-02: bác sĩ xin đổi ca / xin nghỉ")
@RestController
@RequestMapping("/api/booking/bac-si/toi/yeu-cau-doi-lich")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class YeuCauDoiLichCuaBacSiController {

    private final YeuCauDoiLichService yeuCauDoiLichService;

    @Operation(summary = "Gửi yêu cầu xin nghỉ (XIN_NGHI) hoặc đổi ca (DOI_CA, kèm ngày / giờ / phòng mong muốn) cho 1 ca của"
            + " tôi, trước giờ bắt đầu ca ít nhất 24 giờ; mỗi ca chỉ có 1 yêu cầu chờ duyệt")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<YeuCauDoiLichResponse> gui(@Valid @RequestBody GuiYeuCauDoiLichRequest request) {
        return PhanHoiApi.ok(yeuCauDoiLichService.gui(NguoiDungHienTai.layIdTaiKhoan(), request), "Đã gửi yêu cầu");
    }

    @Operation(summary = "Các yêu cầu tôi đã gửi, mới nhất trước; lọc theo trạng thái")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<YeuCauDoiLichResponse>> cuaToi(
            @RequestParam(required = false) TrangThaiYeuCau trangThai,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(yeuCauDoiLichService.cuaToi(NguoiDungHienTai.layIdTaiKhoan(), trangThai, trang,
                kichThuoc));
    }

    @Operation(summary = "Rút 1 yêu cầu còn chờ duyệt của tôi (thành DA_RUT); sau đó ca nhận được yêu cầu mới")
    @PostMapping("/{id}/rut")
    public PhanHoiApi<YeuCauDoiLichResponse> rut(@PathVariable Long id) {
        return PhanHoiApi.ok(yeuCauDoiLichService.rut(NguoiDungHienTai.layIdTaiKhoan(), id), "Đã rút yêu cầu");
    }

}
