package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.HangDoiResponse;
import iuh.fit.se.eclinic.booking.service.HangDoiService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import lombok.RequiredArgsConstructor;

/** Hàng đợi khám trong ngày theo ca: xem, gọi bệnh nhân tiếp theo, đánh dấu vắng mặt. */
@Tag(name = "Hàng đợi khám", description = "Hàng đợi khám trong ngày theo ca của bác sĩ")
@RestController
@RequestMapping("/api/booking/bac-si/toi")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class HangDoiController {

    private final HangDoiService hangDoiService;

    @Operation(summary = "Hàng đợi khám 1 ngày của tôi (mặc định hôm nay) theo ca: số lượng theo trạng thái, người đang"
            + " khám, người được gọi tiếp (chỉ hôm nay), ca nên mở sẵn")
    @GetMapping("/hang-doi")
    public PhanHoiApi<HangDoiResponse> hangDoi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        return PhanHoiApi.ok(hangDoiService.hangDoi(NguoiDungHienTai.layIdTaiKhoan(), ngay));
    }

    @Operation(summary = "Gọi bệnh nhân chờ khám có số thứ tự nhỏ nhất của 1 ca hôm nay của tôi. 404 ca không có / không"
            + " phải của tôi; 409 KHONG_PHAI_NGAY_KHAM, HANG_DOI_TRONG, DANG_KHAM_BENH_NHAN_KHAC")
    @PostMapping("/hang-doi/goi-tiep")
    public PhanHoiApi<HangDoiResponse> goiTiep(@RequestParam Long idLichLamViec) {
        return PhanHoiApi.ok(hangDoiService.goiTiep(NguoiDungHienTai.layIdTaiKhoan(), idLichLamViec));
    }

    @Operation(summary = "Gọi 1 bệnh nhân vào khám (chờ khám hoặc gọi lại người vắng mặt; gọi lại người đang khám vẫn"
            + " 200). 404 lịch hẹn không có / không phải của tôi; 409 KHONG_PHAI_NGAY_KHAM, LICH_HEN_KHONG_KHAM_DUOC,"
            + " DANG_KHAM_BENH_NHAN_KHAC")
    @PostMapping("/lich-hen/{id}/goi-kham")
    public PhanHoiApi<HangDoiResponse> goi(@PathVariable Long id) {
        return PhanHoiApi.ok(hangDoiService.goi(NguoiDungHienTai.layIdTaiKhoan(), id));
    }

    @Operation(summary = "Đánh dấu bệnh nhân vắng mặt (gọi lại được sau; đã vắng mặt vẫn 200). 404 lịch hẹn không có /"
            + " không phải của tôi; 409 KHONG_PHAI_NGAY_KHAM, LICH_HEN_KHONG_KHAM_DUOC")
    @PostMapping("/lich-hen/{id}/vang-mat")
    public PhanHoiApi<HangDoiResponse> vangMat(@PathVariable Long id) {
        return PhanHoiApi.ok(hangDoiService.vangMat(NguoiDungHienTai.layIdTaiKhoan(), id));
    }

}
