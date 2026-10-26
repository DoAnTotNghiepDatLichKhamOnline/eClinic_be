package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import iuh.fit.se.eclinic.booking.dto.request.KhaiBaoNgayNghiRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhaiBaoNgayNghiResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiQuanTriResponse;
import iuh.fit.se.eclinic.booking.service.NgayNghiService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Quản trị viên khai báo ngày bệnh viện nghỉ (Tết, lễ, nghỉ đột xuất). */
@Tag(name = "Quản trị ngày nghỉ", description = "Ngày bệnh viện nghỉ: khai báo (có xem trước), danh sách, bỏ ngày nghỉ")
@RestController
@RequestMapping("/api/booking/quan-tri/ngay-nghi")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriNgayNghiController {

    private final NgayNghiService ngayNghiService;

    @Operation(summary = "Ngày nghỉ trong khoảng (mặc định từ hôm nay, 1 năm; tối đa 366 ngày), theo ngày, kèm 2 bản lý"
            + " do và người khai báo")
    @GetMapping
    public PhanHoiApi<List<NgayNghiQuanTriResponse>> danhSach(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return PhanHoiApi.ok(ngayNghiService.danhSach(tuNgay, denNgay));
    }

    @Operation(summary = "Xem trước khai báo: chạy đúng như khi lưu rồi hoàn tác, trả các ngày mới, ngày đã nghỉ từ trước"
            + " và các ca sẽ bị hủy kèm số lịch hẹn phải đổi. Không lưu gì, không báo ai")
    @PostMapping("/xem-truoc")
    public PhanHoiApi<KetQuaKhaiBaoNgayNghiResponse> xemTruoc(@Valid @RequestBody KhaiBaoNgayNghiRequest request) {
        return PhanHoiApi.ok(ngayNghiService.khaiBao(NguoiDungHienTai.layIdTaiKhoan(), request, false));
    }

    @Operation(summary = "Khai báo bệnh viện nghỉ [tuNgay, denNgay] (từ ngày mai, tối đa 42 ngày; ngày đã nghỉ được bỏ"
            + " qua, mọi ngày đều đã nghỉ -> 409 NGAY_BENH_VIEN_NGHI). Các ca còn hoạt động bị hủy: lịch hẹn giữ nguyên,"
            + " được đánh dấu cần đổi lịch, bệnh nhân được báo và nhận email; mỗi bác sĩ được báo 1 lần")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<KetQuaKhaiBaoNgayNghiResponse> khaiBao(@Valid @RequestBody KhaiBaoNgayNghiRequest request) {
        return PhanHoiApi.ok(ngayNghiService.khaiBao(NguoiDungHienTai.layIdTaiKhoan(), request, true),
                "thanhCong.khaiBaoNgayNghi");
    }

    @Operation(summary = "Bỏ 1 ngày nghỉ chưa tới (hôm nay / đã qua -> 400). Các ca đã bị hủy vẫn hủy; ngày đó xếp ca lại"
            + " được")
    @DeleteMapping("/{ngay}")
    public PhanHoiApi<Void> xoa(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        ngayNghiService.xoa(NguoiDungHienTai.layIdTaiKhoan(), ngay);
        return PhanHoiApi.ok(null, "thanhCong.xoaNgayNghi");
    }

}
