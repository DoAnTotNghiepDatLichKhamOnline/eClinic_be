package iuh.fit.se.eclinic.booking.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import iuh.fit.se.eclinic.booking.dto.request.DoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.HuyLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;
import iuh.fit.se.eclinic.booking.service.GioiHanDatLichService;
import iuh.fit.se.eclinic.booking.service.HuyDoiLichService;
import iuh.fit.se.eclinic.booking.service.PhieuKhamService;
import iuh.fit.se.eclinic.booking.util.DiaChiIp;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * BOOK-05, BOOK-06: phiếu khám và mã QR, tra bằng mã ngẫu nhiên trả về khi đặt lịch (quy tắc #8). Công khai
 * (app.bao-mat.duong-dan-cong-khai): ai có link đều xem được nên CCCD, SĐT đã che; không nhận id lịch hẹn.
 */
@Tag(name = "Phiếu khám", description = "BOOK-05, BOOK-06: xem phiếu khám và mã QR bằng mã phiếu khám")
@RestController
@RequestMapping("/api/booking/phieu-kham")
@RequiredArgsConstructor
public class PhieuKhamController {

    private static final String TEN_TEP_QR = "phieu-kham-qr.png";

    private final PhieuKhamService phieuKhamService;
    private final HuyDoiLichService huyDoiLichService;
    private final GioiHanDatLichService gioiHanDatLichService;

    @Operation(summary = "Phiếu khám: số thứ tự, giờ khám dự kiến, bác sĩ, phòng, trạng thái; CCCD và SĐT đã che")
    @GetMapping("/{maPhieuKham}")
    public PhanHoiApi<PhieuKhamResponse> xem(@PathVariable String maPhieuKham) {
        return PhanHoiApi.ok(phieuKhamService.xem(maPhieuKham));
    }

    @Operation(summary = "Hủy lịch hẹn bằng link phiếu khám: phải gửi đúng SĐT liên hệ đã nhập lúc đặt lịch; lý do không"
            + " bắt buộc; số lần gọi từ 1 địa chỉ IP bị giới hạn")
    @PostMapping("/{maPhieuKham}/huy")
    public PhanHoiApi<PhieuKhamResponse> huy(@PathVariable String maPhieuKham,
            @Valid @RequestBody HuyLichRequest request, HttpServletRequest httpRequest) {
        gioiHanDatLichService.ghiNhan(DiaChiIp.cua(httpRequest));
        return PhanHoiApi.ok(huyDoiLichService.huyTheoPhieu(maPhieuKham, request), "Đã hủy lịch hẹn");
    }

    @Operation(summary = "Đổi lịch hẹn bằng link phiếu khám (kèm SĐT liên hệ) sang khung giờ khác: trả lịch mới (chờ xác"
            + " nhận) với phiếu khám mới; số lần gọi từ 1 địa chỉ IP bị giới hạn")
    @PostMapping("/{maPhieuKham}/doi-lich")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<DatLichResponse> doiLich(@PathVariable String maPhieuKham,
            @Valid @RequestBody DoiLichRequest request, HttpServletRequest httpRequest) {
        gioiHanDatLichService.ghiNhan(DiaChiIp.cua(httpRequest));
        return PhanHoiApi.ok(huyDoiLichService.doiTheoPhieu(maPhieuKham, request), "Đổi lịch thành công");
    }

    /**
     * Không khai báo {@code produces = image/png}: khi mã không tồn tại, lỗi vẫn phải trả về dạng JSON của PhanHoiApi.
     */
    @Operation(summary = "Ảnh PNG mã QR chứa link phiếu khám; taiVe=true để trình duyệt lưu thành tệp")
    @GetMapping("/{maPhieuKham}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable String maPhieuKham,
            @RequestParam(required = false) @Min(100) @Max(1000) Integer kichThuoc,
            @RequestParam(defaultValue = "false") boolean taiVe) {
        byte[] anh = phieuKhamService.taoQr(maPhieuKham, kichThuoc);
        ResponseEntity.BodyBuilder phanHoi = ResponseEntity.ok().contentType(MediaType.IMAGE_PNG);
        if (taiVe) {
            phanHoi.header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(TEN_TEP_QR).build().toString());
        }
        return phanHoi.body(anh);
    }

}
