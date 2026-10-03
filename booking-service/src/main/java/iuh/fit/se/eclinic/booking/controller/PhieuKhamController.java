package iuh.fit.se.eclinic.booking.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;
import iuh.fit.se.eclinic.booking.service.PhieuKhamService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
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

    @Operation(summary = "Phiếu khám: số thứ tự, giờ khám dự kiến, bác sĩ, phòng, trạng thái; CCCD và SĐT đã che")
    @GetMapping("/{maPhieuKham}")
    public PhanHoiApi<PhieuKhamResponse> xem(@PathVariable String maPhieuKham) {
        return PhanHoiApi.ok(phieuKhamService.xem(maPhieuKham));
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
