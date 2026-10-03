package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.CaKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayConChoResponse;
import iuh.fit.se.eclinic.booking.service.TraCuuLichKhamService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import lombok.RequiredArgsConstructor;

/**
 * SCHED-04, BOOK-12: xem khung giờ khám kèm số chỗ còn lại khi đặt lịch. Công khai (app.bao-mat.duong-dan-cong-khai).
 * Lọc theo idBacSi hoặc idChuyenKhoa (mọi bác sĩ của chuyên khoa); ngày theo dạng yyyy-MM-dd.
 */
@Tag(name = "Khung giờ khám", description = "SCHED-04, BOOK-12: khung giờ 1 tiếng kèm số chỗ còn lại")
@RestController
@RequestMapping("/api/booking/khung-gio")
@RequiredArgsConstructor
public class KhungGioController {

    private final TraCuuLichKhamService traCuuLichKhamService;

    @Operation(summary = "Các ca và khung giờ 1 tiếng của 1 ngày, theo bác sĩ hoặc theo chuyên khoa")
    @GetMapping
    public PhanHoiApi<List<CaKhamResponse>> timTheoNgay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) Long idBacSi,
            @RequestParam(required = false) Long idChuyenKhoa) {
        return PhanHoiApi.ok(traCuuLichKhamService.timKhungGioTheoNgay(ngay, idBacSi, idChuyenKhoa));
    }

    @Operation(summary = "Các ngày có ca làm việc kèm số chỗ còn lại (0 = hết chỗ)")
    @GetMapping("/ngay-con-cho")
    public PhanHoiApi<List<NgayConChoResponse>> timNgayConCho(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idBacSi,
            @RequestParam(required = false) Long idChuyenKhoa) {
        return PhanHoiApi.ok(traCuuLichKhamService.timNgayConCho(tuNgay, denNgay, idBacSi, idChuyenKhoa));
    }

}
