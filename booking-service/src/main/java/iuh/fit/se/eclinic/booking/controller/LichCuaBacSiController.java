package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import iuh.fit.se.eclinic.booking.dto.request.BacSiHuyLichHenRequest;
import iuh.fit.se.eclinic.booking.dto.request.CapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.CapLaiLienKetResponse;
import iuh.fit.se.eclinic.booking.dto.response.HoSoKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.service.BacSiHuyLichHenService;
import iuh.fit.se.eclinic.booking.service.CapLaiLienKetService;
import iuh.fit.se.eclinic.booking.service.HoSoKhamService;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.PdfLichLamViecService;
import iuh.fit.se.eclinic.booking.service.PdfLichLamViecService.TepPdf;
import iuh.fit.se.eclinic.booking.util.TepTaiVe;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * SCHED-04: bác sĩ xem (và tải bản in PDF) lịch làm việc và lịch hẹn của CHÍNH MÌNH, mở hồ sơ khám của bệnh nhân từ 1 lịch hẹn của mình, và
 * sửa hồ sơ bệnh nhân / ghi nhận đã đối chiếu giấy tờ cho lịch hẹn đó, cấp lại liên kết phiếu khám cho người đã mất
 * link (PAT-03). Không nhận id bác sĩ: bác sĩ lấy từ tài khoản
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
    private final BacSiHuyLichHenService bacSiHuyLichHenService;
    private final PdfLichLamViecService pdfLichLamViecService;
    private final CapLaiLienKetService capLaiLienKetService;

    @Operation(summary = "Các ca làm việc của tôi trong khoảng ngày (tối đa 42 ngày, kể cả ca đã huỷ) kèm số lượt đã đặt")
    @GetMapping("/lich-lam-viec")
    public PhanHoiApi<List<CaLamViecResponse>> lichLamViec(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return PhanHoiApi.ok(lichLamViecService.lichCuaBacSi(NguoiDungHienTai.layIdTaiKhoan(), tuNgay, denNgay));
    }

    // Không khai báo produces: lỗi (khoảng ngày sai...) vẫn trả JSON như các API khác
    @Operation(summary = "Tải bản in PDF lịch làm việc của tôi trong khoảng ngày (tối đa 42 ngày, chỉ ca còn hoạt động),"
            + " chữ theo ngôn ngữ của request")
    @GetMapping("/lich-lam-viec/pdf")
    public ResponseEntity<byte[]> pdfLichLamViec(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        TepPdf tep = pdfLichLamViecService.cuaToi(NguoiDungHienTai.layIdTaiKhoan(), tuNgay, denNgay);
        return TepTaiVe.taiVe(tep.noiDung(), tep.tenTep(), MediaType.APPLICATION_PDF);
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

    @Operation(summary = "Hủy 1 lịch hẹn đã xác nhận của tôi (trước giờ khám), bắt buộc có lý do; lượt khám được mở lại"
            + " cho người khác đặt, bệnh nhân được báo và đọc được lý do trên phiếu khám")
    @PostMapping("/lich-hen/{id}/huy")
    public PhanHoiApi<LichHenTrongCaResponse> huy(@PathVariable Long id,
            @Valid @RequestBody BacSiHuyLichHenRequest request) {
        return PhanHoiApi.ok(bacSiHuyLichHenService.huy(NguoiDungHienTai.layIdTaiKhoan(), id, request.lyDo()),
                "thanhCong.bacSiHuyLichHen");
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
                "thanhCong.capNhatHoSoBenhNhan");
    }

    @Operation(summary = "Đã đối chiếu giấy tờ: bỏ đánh dấu cần đối chiếu của 1 lịch hẹn của tôi (gọi lại vẫn 200)")
    @PostMapping("/lich-hen/{id}/da-doi-chieu")
    public PhanHoiApi<Void> daDoiChieu(@PathVariable Long id) {
        hoSoKhamService.daDoiChieu(NguoiDungHienTai.layIdTaiKhoan(), id);
        return PhanHoiApi.ok(null, "thanhCong.doiChieuBenhNhan");
    }

    @Operation(summary = "Lịch hẹn của tôi (mọi trạng thái, tối đa 50, giờ khám muộn nhất trước) mà người khám hoặc người"
            + " giám hộ có số CCCD này: tìm lịch hẹn của người đến xin cấp lại liên kết")
    @GetMapping("/lich-hen/theo-cccd")
    public PhanHoiApi<List<LichHenTrongCaResponse>> lichHenTheoCccd(
            @RequestParam @Pattern(regexp = "\\d{12}", message = "{kiemTra.cccd.dinhDang}") String cccd) {
        return PhanHoiApi.ok(capLaiLienKetService.bacSiTimTheoCccd(NguoiDungHienTai.layIdTaiKhoan(), cccd));
    }

    @Operation(summary = "Cấp lại liên kết phiếu khám (PAT-03) cho 1 lịch hẹn của tôi, như quản trị viên: dùng lại mã cũ, lịch"
            + " hẹn hiện hành của chuỗi đổi lịch, cccd của bệnh nhân / người giám hộ (sai -> 403 CCCD_KHONG_KHOP),"
            + " cachGui=EMAIL (409 KHONG_CO_EMAIL_NHAN khi không có email) hoặc MAN_HINH (trả mã + link để mở QR)."
            + " Lịch hẹn của bác sĩ khác -> 404")
    @PostMapping("/lich-hen/{id}/cap-lai-lien-ket")
    public PhanHoiApi<CapLaiLienKetResponse> capLaiLienKet(@PathVariable Long id,
            @Valid @RequestBody CapLaiLienKetRequest request) {
        return PhanHoiApi.ok(capLaiLienKetService.bacSi(NguoiDungHienTai.layIdTaiKhoan(), id, request),
                "thanhCong.capLaiLienKet");
    }

}
