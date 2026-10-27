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
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiResponse;
import iuh.fit.se.eclinic.booking.service.NgayNghiService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import lombok.RequiredArgsConstructor;

/** Ngày bệnh viện nghỉ cho lịch của trang đặt lịch (công khai). */
@Tag(name = "Ngày nghỉ", description = "Ngày bệnh viện nghỉ: làm mờ trên lịch đặt khám, kèm lý do")
@RestController
@RequestMapping("/api/booking/ngay-nghi")
@RequiredArgsConstructor
public class NgayNghiController {

    private final NgayNghiService ngayNghiService;

    @Operation(summary = "Ngày bệnh viện nghỉ trong khoảng (mặc định từ hôm nay, 1 năm; tối đa 366 ngày), lý do theo"
            + " ngôn ngữ của request. Ngày nghỉ không có lượt khám nào để đặt. Không cần đăng nhập")
    @GetMapping
    public PhanHoiApi<List<NgayNghiResponse>> danhSach(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return PhanHoiApi.ok(ngayNghiService.congKhai(tuNgay, denNgay));
    }

}
