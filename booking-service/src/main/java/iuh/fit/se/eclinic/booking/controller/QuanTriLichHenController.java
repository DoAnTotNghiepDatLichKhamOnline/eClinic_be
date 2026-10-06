package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import lombok.RequiredArgsConstructor;

/**
 * ADM-04: quản trị viên tra lịch hẹn theo mã in trên phiếu khám (hỗ trợ bệnh nhân mất phiếu, khiếu nại), và bỏ đánh
 * dấu "cần đối chiếu" của lịch hẹn sau khi đã đối chiếu giấy tờ.
 */
@Tag(name = "Quản trị lịch hẹn", description = "ADM-04: tra lịch hẹn theo mã, đánh dấu đã đối chiếu (quản trị viên)")
@RestController
@RequestMapping("/api/booking/quan-tri/lich-hen")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriLichHenController {

    private final LichLamViecService lichLamViecService;
    private final LichHenService lichHenService;

    @Operation(summary = "Tra 1 lịch hẹn theo mã tra cứu ngắn (ECL-...) hoặc mã phiếu khám")
    @GetMapping("/tra-cuu")
    public PhanHoiApi<LichHenTrongCaResponse> traCuu(@RequestParam String ma) {
        return PhanHoiApi.ok(lichLamViecService.traCuuLichHen(null, ma));
    }

    @Operation(summary = "Đã đối chiếu giấy tờ: bỏ đánh dấu cần đối chiếu của lịch hẹn (gọi lại nhiều lần vẫn 200)")
    @PostMapping("/{id}/da-doi-chieu")
    public PhanHoiApi<Void> daDoiChieu(@PathVariable Long id) {
        lichHenService.danhDauDaDoiChieu(id);
        return PhanHoiApi.ok(null, "Đã ghi nhận đối chiếu thông tin bệnh nhân");
    }

}
