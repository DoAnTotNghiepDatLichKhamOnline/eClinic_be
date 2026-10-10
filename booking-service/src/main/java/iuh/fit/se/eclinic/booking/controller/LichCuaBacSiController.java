package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.request.TuChoiLichHenRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.HoSoKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.service.HoSoKhamService;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.YeuCauLichHenService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * SCHED-04: bác sĩ xem lịch làm việc và lịch hẹn của CHÍNH MÌNH, mở hồ sơ khám của bệnh nhân từ 1 lịch hẹn của mình, và
 * sửa hồ sơ bệnh nhân / ghi nhận đã đối chiếu giấy tờ cho lịch hẹn đó. Không nhận id bác sĩ: bác sĩ lấy từ tài khoản
 * đang đăng nhập; lịch hẹn của bác sĩ khác coi như không có (404). Ngày theo dạng yyyy-MM-dd.
 */
@Tag(name = "Lịch của bác sĩ", description = "SCHED-04: bác sĩ xem lịch làm việc, lịch hẹn, hồ sơ khám của bệnh nhân")
@RestController
@RequestMapping("/api/booking/bac-si/toi")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class LichCuaBacSiController {

    private final LichLamViecService lichLamViecService;
    private final HoSoKhamService hoSoKhamService;
    private final YeuCauLichHenService yeuCauLichHenService;

    @Operation(summary = "Các ca làm việc của tôi trong khoảng ngày (tối đa 42 ngày, kể cả ca đã huỷ) kèm số lượt đã đặt")
    @GetMapping("/lich-lam-viec")
    public PhanHoiApi<List<CaLamViecResponse>> lichLamViec(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return PhanHoiApi.ok(lichLamViecService.lichCuaBacSi(NguoiDungHienTai.layIdTaiKhoan(), tuNgay, denNgay));
    }

    @Operation(summary = "Tra 1 lịch hẹn của tôi theo mã tra cứu ngắn (ECL-...), mã phiếu khám hoặc link phiếu khám (quét QR)")
    @GetMapping("/lich-hen/tra-cuu")
    public PhanHoiApi<LichHenTrongCaResponse> traCuuLichHen(@RequestParam String ma) {
        return PhanHoiApi.ok(lichLamViecService.traCuuLichHen(NguoiDungHienTai.layIdTaiKhoan(), ma));
    }

    @Operation(summary = "Lịch hẹn của tôi trong 1 ngày (mặc định hôm nay) theo giờ khám, mọi trạng thái, kèm thông tin"
            + " bệnh nhân; lọc theo ca, số thứ tự, họ tên bệnh nhân (không xét dấu, hoa/thường)")
    @GetMapping("/lich-hen")
    public PhanHoiApi<List<LichHenTrongCaResponse>> lichHen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) Long idLichLamViec,
            @RequestParam(required = false) Integer soThuTu,
            @RequestParam(required = false) String tuKhoa) {
        return PhanHoiApi.ok(lichLamViecService.lichHenCuaBacSiTheoNgay(NguoiDungHienTai.layIdTaiKhoan(), ngay,
                idLichLamViec, soThuTu, tuKhoa));
    }

    @Operation(summary = "Lịch hẹn của tôi theo khoảng ngày (tối đa 42 ngày, mọi trạng thái, theo giờ khám) cho màn hình"
            + " lịch")
    @GetMapping("/lich-hen/lich")
    public PhanHoiApi<List<LichHenTrongCaResponse>> lichHenTheoKhoang(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return PhanHoiApi.ok(lichLamViecService.lichHenCuaBacSiTrongKhoang(NguoiDungHienTai.layIdTaiKhoan(), tuNgay,
                denNgay));
    }

    @Operation(summary = "Yêu cầu đặt lịch: lịch hẹn đang chờ tôi xác nhận mà lượt khám chưa bắt đầu, giờ khám sớm nhất"
            + " trước")
    @GetMapping("/lich-hen/yeu-cau")
    public PhanHoiApi<TrangDuLieu<LichHenTrongCaResponse>> yeuCau(
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(yeuCauLichHenService.danhSach(NguoiDungHienTai.layIdTaiKhoan(), trang, kichThuoc));
    }

    @Operation(summary = "Xác nhận 1 lịch hẹn đang chờ xác nhận của tôi (trước giờ khám)")
    @PostMapping("/lich-hen/{id}/xac-nhan")
    public PhanHoiApi<LichHenTrongCaResponse> xacNhan(@PathVariable Long id) {
        return PhanHoiApi.ok(yeuCauLichHenService.xacNhan(NguoiDungHienTai.layIdTaiKhoan(), id),
                "Đã xác nhận lịch hẹn");
    }

    @Operation(summary = "Từ chối 1 lịch hẹn đang chờ xác nhận của tôi (trước giờ khám), bắt buộc có lý do; lượt khám"
            + " được mở lại cho người khác đặt")
    @PostMapping("/lich-hen/{id}/tu-choi")
    public PhanHoiApi<LichHenTrongCaResponse> tuChoi(@PathVariable Long id,
            @Valid @RequestBody TuChoiLichHenRequest request) {
        return PhanHoiApi.ok(yeuCauLichHenService.tuChoi(NguoiDungHienTai.layIdTaiKhoan(), id, request.lyDo()),
                "Đã từ chối lịch hẹn");
    }

    @Operation(summary = "Hồ sơ khám của bệnh nhân theo mã tra cứu ngắn, mã phiếu khám hoặc link phiếu khám (quét QR)")
    @GetMapping("/lich-hen/tra-cuu/ho-so-kham")
    public PhanHoiApi<HoSoKhamResponse> hoSoKhamTheoMa(@RequestParam String ma) {
        return PhanHoiApi.ok(hoSoKhamService.theoMa(NguoiDungHienTai.layIdTaiKhoan(), ma));
    }

    @Operation(summary = "Hồ sơ khám của bệnh nhân từ 1 lịch hẹn của tôi: lịch hẹn, hồ sơ bệnh nhân, các lần khám trước")
    @GetMapping("/lich-hen/{id}/ho-so-kham")
    public PhanHoiApi<HoSoKhamResponse> hoSoKham(@PathVariable Long id) {
        return PhanHoiApi.ok(hoSoKhamService.theoLichHen(NguoiDungHienTai.layIdTaiKhoan(), id));
    }

    @Operation(summary = "Sửa hồ sơ bệnh nhân của 1 lịch hẹn của tôi sau khi đối chiếu giấy tờ; số CCCD chỉ điền được khi"
            + " hồ sơ chưa có")
    @PutMapping("/lich-hen/{id}/benh-nhan")
    public PhanHoiApi<HoSoKhamResponse> suaBenhNhan(@PathVariable Long id,
            @Valid @RequestBody SuaHoSoBenhNhanRequest request) {
        return PhanHoiApi.ok(hoSoKhamService.suaBenhNhan(NguoiDungHienTai.layIdTaiKhoan(), id, request),
                "Đã cập nhật hồ sơ bệnh nhân");
    }

    @Operation(summary = "Đã đối chiếu giấy tờ: bỏ đánh dấu cần đối chiếu của 1 lịch hẹn của tôi (gọi lại vẫn 200)")
    @PostMapping("/lich-hen/{id}/da-doi-chieu")
    public PhanHoiApi<Void> daDoiChieu(@PathVariable Long id) {
        hoSoKhamService.daDoiChieu(NguoiDungHienTai.layIdTaiKhoan(), id);
        return PhanHoiApi.ok(null, "Đã ghi nhận đối chiếu thông tin bệnh nhân");
    }

}
