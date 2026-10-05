package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import lombok.RequiredArgsConstructor;

/**
 * ADM-04: quản trị viên xem lịch làm việc toàn viện và lịch hẹn của từng ca. Chỉ đọc (tạo / sửa ca: SCHED-01, chưa
 * làm); ngày theo dạng yyyy-MM-dd.
 */
@Tag(name = "Quản trị lịch làm việc", description = "ADM-04: lịch làm việc toàn viện, lịch hẹn của ca (quản trị viên)")
@RestController
@RequestMapping("/api/booking/quan-tri/lich-lam-viec")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriLichLamViecController {

    private final LichLamViecService lichLamViecService;

    @Operation(summary = "Các ca làm việc trong khoảng ngày (tối đa 42 ngày, kể cả ca đã huỷ) kèm số lượt đã đặt;"
            + " lọc theo chuyên khoa, bác sĩ, phòng khám")
    @GetMapping
    public PhanHoiApi<List<CaLamViecResponse>> lichToanVien(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idChuyenKhoa,
            @RequestParam(required = false) Long idBacSi,
            @RequestParam(required = false) Long idPhongKham) {
        return PhanHoiApi.ok(lichLamViecService.lichToanVien(tuNgay, denNgay, idChuyenKhoa, idBacSi, idPhongKham));
    }

    @Operation(summary = "Lịch hẹn của 1 ca theo giờ khám (mọi trạng thái), kèm thông tin bệnh nhân")
    @GetMapping("/{id}/lich-hen")
    public PhanHoiApi<List<LichHenTrongCaResponse>> lichHenCuaCa(@PathVariable Long id) {
        return PhanHoiApi.ok(lichLamViecService.lichHenCuaCa(id));
    }

}
