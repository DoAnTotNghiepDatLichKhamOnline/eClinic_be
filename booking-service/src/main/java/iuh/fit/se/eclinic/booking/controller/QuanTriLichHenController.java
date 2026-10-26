package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.CapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.request.QuanTriDatLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.QuanTriDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.QuanTriHuyLichHenRequest;
import iuh.fit.se.eclinic.booking.dto.response.CapLaiLienKetResponse;
import iuh.fit.se.eclinic.booking.dto.response.DatLichHotlineResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenQuanTriChiTietResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.service.CapLaiLienKetService;
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.QuanTriLichHenService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * ADM-04: quản trị viên quản lý lịch hẹn toàn viện: danh sách có bộ lọc, chi tiết kèm nhật ký thao tác, hủy / đổi lịch
 * thay bệnh nhân (kèm lý do, không theo hạn hủy / đổi của bệnh nhân); tra lịch hẹn theo mã in trên phiếu khám, bỏ đánh
 * dấu "cần đối chiếu" sau khi đã đối chiếu giấy tờ; đặt lịch thay người gọi tổng đài / người đến quầy; cấp lại liên kết
 * phiếu khám cho người đã mất link (PAT-03). Không trả mã phiếu khám, trừ phản hồi đặt lịch qua tổng đài (cho người vừa
 * đặt in phiếu) và cấp lại liên kết trên màn hình.
 */
@Tag(name = "Quản trị lịch hẹn", description = "ADM-04: danh sách lịch hẹn toàn viện, chi tiết + nhật ký, hủy / đổi lịch"
        + " thay bệnh nhân, tra theo mã, đánh dấu đã đối chiếu, đặt lịch qua tổng đài, cấp lại liên kết (quản trị viên)")
@RestController
@RequestMapping("/api/booking/quan-tri/lich-hen")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriLichHenController {

    private final LichLamViecService lichLamViecService;
    private final LichHenService lichHenService;
    private final QuanTriLichHenService quanTriLichHenService;
    private final CapLaiLienKetService capLaiLienKetService;

    @Operation(summary = "Lịch hẹn toàn viện có giờ khám trong [tuNgay, denNgay] (mặc định từ hôm nay, 31 ngày; tối đa 366"
            + " ngày), sớm nhất trước. Lọc theo chuyên khoa, bác sĩ, phòng, trạng thái, kênh đặt, cần đổi lịch, cần đối"
            + " chiếu; tuKhoa = mã ECL-... đầy đủ, CCCD 12 số (người khám / người giám hộ) hoặc 1 phần họ tên, SĐT, email")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<LichHenTrongCaResponse>> danhSach(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idChuyenKhoa,
            @RequestParam(required = false) Long idBacSi,
            @RequestParam(required = false) Long idPhongKham,
            @RequestParam(required = false) TrangThaiLichHen trangThai,
            @RequestParam(required = false) KenhDatLich kenhDat,
            @RequestParam(required = false) Boolean canDoiLich,
            @RequestParam(required = false) Boolean canDoiChieu,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanTriLichHenService.danhSach(tuNgay, denNgay, idChuyenKhoa, idBacSi, idPhongKham,
                trangThai, kenhDat, canDoiLich, canDoiChieu, tuKhoa, trang, kichThuoc));
    }

    @Operation(summary = "Đặt lịch thay người gọi tổng đài / người đến quầy: cùng form và quy tắc với khách đặt (khung"
            + " giờ, CCCD, người giám hộ, giới hạn theo hồ sơ và SĐT), không giới hạn theo IP; kênh HOTLINE, nhật ký ghi"
            + " quản trị viên. Có email thì phiếu khám được gửi tới đó theo ngonNgu (mặc định VI). Trả id lịch hẹn và"
            + " phiếu khám (kèm mã phiếu khám, chỉ ở phản hồi này) để in hoặc mở mã QR")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<DatLichHotlineResponse> datHotline(@Valid @RequestBody QuanTriDatLichRequest request) {
        return PhanHoiApi.ok(quanTriLichHenService.datHotline(NguoiDungHienTai.layIdTaiKhoan(), request),
                "thanhCong.datLichHotline");
    }

    @Operation(summary = "Tra 1 lịch hẹn theo mã tra cứu ngắn (ECL-...) hoặc mã phiếu khám")
    @GetMapping("/tra-cuu")
    public PhanHoiApi<LichHenTrongCaResponse> traCuu(@RequestParam String ma) {
        return PhanHoiApi.ok(lichLamViecService.traCuuLichHen(null, ma));
    }

    @Operation(summary = "Chi tiết 1 lịch hẹn: email liên hệ, tài khoản đặt, chuỗi đổi lịch và nhật ký thao tác (ai, lúc"
            + " nào, lý do) của cả chuỗi")
    @GetMapping("/{id}")
    public PhanHoiApi<LichHenQuanTriChiTietResponse> chiTiet(@PathVariable Long id) {
        return PhanHoiApi.ok(quanTriLichHenService.chiTiet(id));
    }

    @Operation(summary = "Hủy lịch hẹn kèm lý do (bắt buộc): DA_XAC_NHAN -> QUAN_TRI_HUY, chỗ được mở lại, bệnh nhân được"
            + " báo và nhận email, bác sĩ được báo. Đã quá giờ khám -> 409 LICH_HEN_DA_QUA_GIO (trừ lịch cần đổi vì ca"
            + " bị hủy); không còn DA_XAC_NHAN -> 409 LICH_HEN_KHONG_HUY_DOI_DUOC")
    @PostMapping("/{id}/huy")
    public PhanHoiApi<LichHenQuanTriChiTietResponse> huy(@PathVariable Long id,
            @Valid @RequestBody QuanTriHuyLichHenRequest request) {
        return PhanHoiApi.ok(quanTriLichHenService.huy(NguoiDungHienTai.layIdTaiKhoan(), id, request),
                "thanhCong.quanTriHuyLichHen");
    }

    @Operation(summary = "Đổi lịch hẹn sang khung giờ khác thay bệnh nhân, kèm lý do (bắt buộc): không theo hạn đổi và"
            + " số lần đổi của bệnh nhân; khung giờ mới theo đúng quy tắc đặt lịch. Lịch cũ DA_HUY_DO_DOI_LICH, trả chi"
            + " tiết lịch mới; bệnh nhân được báo và nhận email (ngôn ngữ của lịch cũ), bác sĩ được báo. Cùng điều kiện"
            + " với hủy")
    @PostMapping("/{id}/doi-lich")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<LichHenQuanTriChiTietResponse> doiLich(@PathVariable Long id,
            @Valid @RequestBody QuanTriDoiLichRequest request) {
        return PhanHoiApi.ok(quanTriLichHenService.doiLich(NguoiDungHienTai.layIdTaiKhoan(), id, request),
                "thanhCong.quanTriDoiLich");
    }

    @Operation(summary = "Đã đối chiếu giấy tờ: bỏ đánh dấu cần đối chiếu của lịch hẹn (gọi lại nhiều lần vẫn 200)")
    @PostMapping("/{id}/da-doi-chieu")
    public PhanHoiApi<Void> daDoiChieu(@PathVariable Long id) {
        lichHenService.danhDauDaDoiChieu(id);
        return PhanHoiApi.ok(null, "thanhCong.doiChieuBenhNhan");
    }

    @Operation(summary = "Cấp lại liên kết phiếu khám (PAT-03) cho người đã mất link, dùng lại mã cũ, luôn cho lịch hẹn hiện"
            + " hành của chuỗi đổi lịch (chọn lịch cũ đã đổi thì được lịch mới nhất). cccd = CCCD trên giấy tờ người đó"
            + " xuất trình: của bệnh nhân hoặc người giám hộ trên lịch hẹn, sai -> 403 CCCD_KHONG_KHOP. cachGui=EMAIL: gửi"
            + " tới email của lịch hẹn, trả địa chỉ đã che; không có email -> 409 KHONG_CO_EMAIL_NHAN. cachGui=MAN_HINH:"
            + " trả maPhieuKham + link (và maSoKham + link nếu đã khám) để mở QR qua /phieu-kham/{ma}/qr, /so-kham/{ma}/qr."
            + " Ghi nhật ký CAP_LAI_LIEN_KET")
    @PostMapping("/{id}/cap-lai-lien-ket")
    public PhanHoiApi<CapLaiLienKetResponse> capLaiLienKet(@PathVariable Long id,
            @Valid @RequestBody CapLaiLienKetRequest request) {
        return PhanHoiApi.ok(capLaiLienKetService.quanTri(NguoiDungHienTai.layIdTaiKhoan(), id, request),
                "thanhCong.capLaiLienKet");
    }

}
