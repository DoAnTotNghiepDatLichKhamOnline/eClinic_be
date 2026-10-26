package iuh.fit.se.eclinic.booking.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.XemSoKhamRequest;
import iuh.fit.se.eclinic.booking.dto.response.SoKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.SoKhamTomTatResponse;
import iuh.fit.se.eclinic.booking.service.SoKhamService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** Sổ khám bệnh xem bằng mã ngẫu nhiên của hồ sơ bệnh án. */
@Tag(name = "Sổ khám bệnh", description = "EXAM-03: tóm tắt, kết quả khám (kèm 4 số cuối SĐT) và mã QR bằng mã sổ khám")
@RestController
@RequestMapping("/api/booking/so-kham")
@RequiredArgsConstructor
public class SoKhamController {

    private static final String TEN_TEP_QR = "so-kham-qr.png";

    private final SoKhamService soKhamService;

    @Operation(summary = "Tóm tắt sổ khám: họ tên đã che (N. V. An), năm sinh, ngày giờ khám, bác sĩ, chuyên khoa, phòng;"
            + " không có kết quả khám. Mã sai / không có: 404")
    @GetMapping("/{maSoKham}")
    public PhanHoiApi<SoKhamTomTatResponse> tomTat(@PathVariable String maSoKham) {
        return PhanHoiApi.ok(soKhamService.tomTat(maSoKham));
    }

    @Operation(summary = "Sổ khám đầy đủ (chẩn đoán, ICD-10, sinh hiệu, đơn thuốc, lời dặn, ngày tái khám) khi bonSoCuoi"
            + " khớp 4 số cuối SĐT liên hệ lúc đặt lịch (lượt khám có người giám hộ: SĐT người giám hộ). Sai: 403"
            + " SO_DIEN_THOAI_KHONG_KHOP; sai 5 lần: 429 GUI_LAI_QUA_NHANH trong 15 phút, kể cả khi nhập đúng")
    @PostMapping("/{maSoKham}/xem")
    public PhanHoiApi<SoKhamResponse> xem(@PathVariable String maSoKham,
            @Valid @RequestBody XemSoKhamRequest request) {
        return PhanHoiApi.ok(soKhamService.xem(maSoKham, request.bonSoCuoi()));
    }

    /** Không khai báo {@code produces = image/png} để lỗi vẫn trả về JSON của PhanHoiApi. */
    @Operation(summary = "Ảnh PNG mã QR chứa link sổ khám; taiVe=true để trình duyệt lưu thành tệp")
    @GetMapping("/{maSoKham}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable String maSoKham,
            @RequestParam(required = false) @Min(100) @Max(1000) Integer kichThuoc,
            @RequestParam(defaultValue = "false") boolean taiVe) {
        byte[] anh = soKhamService.taoQr(maSoKham, kichThuoc);
        ResponseEntity.BodyBuilder phanHoi = ResponseEntity.ok().contentType(MediaType.IMAGE_PNG);
        if (taiVe) {
            phanHoi.header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(TEN_TEP_QR).build().toString());
        }
        return phanHoi.body(anh);
    }

}
