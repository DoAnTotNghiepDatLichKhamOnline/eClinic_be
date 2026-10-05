package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import lombok.RequiredArgsConstructor;

/**
 * SCHED-04: bác sĩ xem lịch làm việc và lịch hẹn của CHÍNH MÌNH. Không nhận id bác sĩ: bác sĩ lấy từ tài khoản đang
 * đăng nhập. Chỉ đọc; ngày theo dạng yyyy-MM-dd.
 */
@Tag(name = "Lịch của bác sĩ", description = "SCHED-04: bác sĩ xem lịch làm việc, lịch hẹn của mình")
@RestController
@RequestMapping("/api/booking/bac-si/toi")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class LichCuaBacSiController {

    private final LichLamViecService lichLamViecService;

    @Operation(summary = "Các ca làm việc của tôi trong khoảng ngày (tối đa 42 ngày, kể cả ca đã huỷ) kèm số lượt đã đặt")
    @GetMapping("/lich-lam-viec")
    public PhanHoiApi<List<CaLamViecResponse>> lichLamViec(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return PhanHoiApi.ok(lichLamViecService.lichCuaBacSi(NguoiDungHienTai.layIdTaiKhoan(), tuNgay, denNgay));
    }

    @Operation(summary = "Tra 1 lịch hẹn của tôi theo mã tra cứu ngắn (ECL-...) hoặc mã phiếu khám (quét QR)")
    @GetMapping("/lich-hen/tra-cuu")
    public PhanHoiApi<LichHenTrongCaResponse> traCuuLichHen(@RequestParam String ma) {
        return PhanHoiApi.ok(lichLamViecService.traCuuLichHen(NguoiDungHienTai.layIdTaiKhoan(), ma));
    }

    @Operation(summary = "Lịch hẹn của tôi trong 1 ngày theo giờ khám (mọi trạng thái), kèm thông tin bệnh nhân")
    @GetMapping("/lich-hen")
    public PhanHoiApi<List<LichHenTrongCaResponse>> lichHen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        return PhanHoiApi.ok(lichLamViecService.lichHenCuaBacSiTheoNgay(NguoiDungHienTai.layIdTaiKhoan(), ngay));
    }

}
