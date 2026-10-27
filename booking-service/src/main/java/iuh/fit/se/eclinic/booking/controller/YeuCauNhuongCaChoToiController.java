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

/** Bác sĩ được nhường ca xem, đồng ý hoặc từ chối yêu cầu gửi cho mình. */
@Tag(name = "Yêu cầu nhường ca gửi cho tôi", description = "SCHED-03: bác sĩ nhận ca đồng ý / từ chối")
@RestController
@RequestMapping("/api/booking/bac-si/toi/yeu-cau-nhuong-ca")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class YeuCauNhuongCaChoToiController {

    private final YeuCauDoiLichService yeuCauDoiLichService;

    @Operation(summary = "Yêu cầu nhường ca gửi cho tôi, mới nhất trước; lọc theo trạng thái (CHO_DONG_Y = đang chờ tôi)")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<YeuCauDoiLichResponse>> guiChoToi(
            @RequestParam(required = false) TrangThaiYeuCau trangThai,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(yeuCauDoiLichService.guiChoToi(NguoiDungHienTai.layIdTaiKhoan(), trangThai, trang,
                kichThuoc));
    }

    @Operation(summary = "Đồng ý nhận ca: yêu cầu thành CHO_DUYET và chờ quản trị viên duyệt. Tôi trùng giờ / vượt giờ làm"
            + " trong tuần -> 409 TRUNG_LICH_LAM_VIEC / VUOT_GIO_LAM_TUAN")
    @PostMapping("/{id}/dong-y")
    public PhanHoiApi<YeuCauDoiLichResponse> dongY(@PathVariable Long id) {
        return PhanHoiApi.ok(yeuCauDoiLichService.dongY(NguoiDungHienTai.layIdTaiKhoan(), id), "thanhCong.dongYNhanCa");
    }

    @Operation(summary = "Từ chối nhận ca (ghi chú tùy chọn): yêu cầu thành DONG_NGHIEP_TU_CHOI")
    @PostMapping("/{id}/tu-choi")
    public PhanHoiApi<YeuCauDoiLichResponse> tuChoi(@PathVariable Long id,
            @Valid @RequestBody(required = false) XuLyYeuCauRequest request) {
        return PhanHoiApi.ok(yeuCauDoiLichService.tuChoiNhan(NguoiDungHienTai.layIdTaiKhoan(), id,
                request == null ? null : request.ghiChu()), "thanhCong.tuChoiNhanCa");
    }

}
