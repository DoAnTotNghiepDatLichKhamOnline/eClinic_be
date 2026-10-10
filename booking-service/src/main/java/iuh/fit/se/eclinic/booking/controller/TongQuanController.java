package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanBacSiResponse;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanQuanTriResponse;
import iuh.fit.se.eclinic.booking.service.TongQuanService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import lombok.RequiredArgsConstructor;

/** Số liệu của 2 màn hình Dashboard (bác sĩ, quản trị viên): mỗi màn hình 1 lần gọi. */
@Tag(name = "Dashboard", description = "Các ô số liệu của Dashboard bác sĩ và Dashboard quản trị viên")
@RestController
@RequiredArgsConstructor
public class TongQuanController {

    private final TongQuanService tongQuanService;

    @Operation(summary = "Dashboard của bác sĩ: lịch hẹn hôm nay, đang chờ, đã khám, điểm đánh giá trung bình + số lượt, số"
            + " yêu cầu chờ xác nhận")
    @GetMapping("/api/booking/bac-si/toi/tong-quan")
    @PreAuthorize("hasRole('BAC_SI')")
    public PhanHoiApi<TongQuanBacSiResponse> cuaBacSi() {
        return PhanHoiApi.ok(tongQuanService.cuaBacSi(NguoiDungHienTai.layIdTaiKhoan()));
    }

    @Operation(summary = "Dashboard của quản trị viên: bác sĩ đang công tác, tài khoản bệnh nhân đã kích hoạt, lượt đặt lịch"
            + " trong tháng này")
    @GetMapping("/api/booking/quan-tri/tong-quan")
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public PhanHoiApi<TongQuanQuanTriResponse> cuaQuanTri() {
        return PhanHoiApi.ok(tongQuanService.cuaQuanTri());
    }
}
