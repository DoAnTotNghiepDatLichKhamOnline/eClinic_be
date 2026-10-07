package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.HuyCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.SuaCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.TaoCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaTaoCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * ADM-04: quản trị viên xem lịch làm việc toàn viện và lịch hẹn của từng ca. Chỉ đọc (tạo / sửa ca: SCHED-01, chưa
 * làm); ngày theo dạng yyyy-MM-dd.
 */
@Tag(name = "Quản trị lịch làm việc", description = "ADM-04, SCHED-01/05/06: lịch làm việc toàn viện, xếp / sửa / hủy ca,"
        + " lịch hẹn của ca (quản trị viên)")
@RestController
@RequestMapping("/api/booking/quan-tri/lich-lam-viec")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriLichLamViecController {

    private final LichLamViecService lichLamViecService;
    private final QuanLyCaService quanLyCaService;

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

    @Operation(summary = "Xếp 1 ca làm việc (sinh các lượt khám), có thể lặp lại theo thứ trong tuần tới 1 ngày; ngày trùng"
            + " giờ với ca khác của bác sĩ / phòng khám bị bỏ qua và liệt kê trong boQua")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<KetQuaTaoCaResponse> tao(@Valid @RequestBody TaoCaRequest request) {
        return PhanHoiApi.ok(quanLyCaService.tao(NguoiDungHienTai.layIdTaiKhoan(), request), "Đã xếp ca làm việc");
    }

    @Operation(summary = "Sửa phòng khám, giờ, sức chứa của 1 ca chưa bắt đầu. Phòng đổi được bất cứ lúc nào; giờ / sức chứa"
            + " chỉ đổi được khi mọi lượt đã có người đặt còn nguyên (không thì 409 CA_CON_LICH_HEN)")
    @PutMapping("/{id}")
    public PhanHoiApi<CaLamViecResponse> sua(@PathVariable Long id, @Valid @RequestBody SuaCaRequest request) {
        return PhanHoiApi.ok(quanLyCaService.sua(NguoiDungHienTai.layIdTaiKhoan(), id, request),
                "Đã sửa ca làm việc");
    }

    @Operation(summary = "Hủy 1 ca chưa bắt đầu (bắt buộc có lý do). Lịch hẹn còn hiệu lực của ca được giữ và đánh dấu cần"
            + " đổi lịch, bệnh nhân và bác sĩ được báo")
    @PostMapping("/{id}/huy")
    public PhanHoiApi<KetQuaHuyCaResponse> huy(@PathVariable Long id, @Valid @RequestBody HuyCaRequest request) {
        return PhanHoiApi.ok(quanLyCaService.huy(NguoiDungHienTai.layIdTaiKhoan(), id, request.lyDo()),
                "Đã hủy ca làm việc");
    }

    @Operation(summary = "Lịch hẹn còn hiệu lực đang chờ bệnh nhân đổi lịch vì ca khám bị hủy, giờ khám cũ sớm nhất trước")
    @GetMapping("/can-doi-lich")
    public PhanHoiApi<TrangDuLieu<LichHenTrongCaResponse>> lichHenCanDoi(
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanLyCaService.lichHenCanDoi(trang, kichThuoc));
    }

    @Operation(summary = "Lịch hẹn của 1 ca theo giờ khám (mọi trạng thái), kèm thông tin bệnh nhân")
    @GetMapping("/{id}/lich-hen")
    public PhanHoiApi<List<LichHenTrongCaResponse>> lichHenCuaCa(@PathVariable Long id) {
        return PhanHoiApi.ok(lichLamViecService.lichHenCuaCa(id));
    }

}
