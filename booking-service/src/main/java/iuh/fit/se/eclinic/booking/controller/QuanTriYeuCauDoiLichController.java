package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.XuLyYeuCauRequest;
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
 * SCHED-03: quản trị viên xem và xử lý yêu cầu đổi ca / xin nghỉ của bác sĩ. Duyệt thì lịch làm việc đổi theo trong cùng
 * 1 transaction (xem {@link YeuCauDoiLichService#duyet}).
 */
@Tag(name = "Quản trị yêu cầu đổi lịch", description = "SCHED-03: duyệt / từ chối yêu cầu đổi ca, xin nghỉ")
@RestController
@RequestMapping("/api/booking/quan-tri/yeu-cau-doi-lich")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriYeuCauDoiLichController {

    private final YeuCauDoiLichService yeuCauDoiLichService;

    @Operation(summary = "Yêu cầu của mọi bác sĩ, gửi sớm nhất trước; mặc định chỉ yêu cầu chờ duyệt, tatCa=true để lấy mọi"
            + " trạng thái. Mỗi dòng kèm ca và số lượt đã có người đặt của ca")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<YeuCauDoiLichResponse>> danhSach(
            @RequestParam(defaultValue = "CHO_DUYET") TrangThaiYeuCau trangThai,
            @RequestParam(defaultValue = "false") boolean tatCa,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(yeuCauDoiLichService.danhSach(tatCa ? null : trangThai, trang, kichThuoc));
    }

    @Operation(summary = "Duyệt: xin nghỉ thì hủy ca; đổi ca thì đổi phòng tại chỗ hoặc hủy ca cũ và xếp ca mới. Ca mới trùng"
            + " giờ -> 409 TRUNG_LICH_LAM_VIEC, yêu cầu vẫn chờ duyệt")
    @PostMapping("/{id}/duyet")
    public PhanHoiApi<YeuCauDoiLichResponse> duyet(@PathVariable Long id,
            @Valid @RequestBody(required = false) XuLyYeuCauRequest request) {
        return PhanHoiApi.ok(yeuCauDoiLichService.duyet(NguoiDungHienTai.layIdTaiKhoan(), id,
                request == null ? null : request.ghiChu()), "Đã duyệt yêu cầu");
    }

    @Operation(summary = "Từ chối, bắt buộc có ghi chú")
    @PostMapping("/{id}/tu-choi")
    public PhanHoiApi<YeuCauDoiLichResponse> tuChoi(@PathVariable Long id,
            @Valid @RequestBody(required = false) XuLyYeuCauRequest request) {
        return PhanHoiApi.ok(yeuCauDoiLichService.tuChoi(NguoiDungHienTai.layIdTaiKhoan(), id,
                request == null ? null : request.ghiChu()), "Đã từ chối yêu cầu");
    }

}
