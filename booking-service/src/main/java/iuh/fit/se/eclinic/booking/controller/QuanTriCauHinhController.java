package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.CapNhatCauHinhRequest;
import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichSuCauHinhResponse;
import iuh.fit.se.eclinic.booking.service.CauHinhService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Quản trị viên sửa quy tắc đặt lịch / xếp ca (SCHED-05, quy tắc #13): đặt trước tối thiểu / tối đa, số lịch còn hiệu
 * lực, hạn hủy / đổi lịch, số lần đổi lịch, hạn gửi yêu cầu đổi ca, số ngày xếp ca trước, giờ làm tối đa mỗi tuần, hạn
 * sửa đánh giá. Giá trị mới áp dụng ngay cho thao tác mới.
 */
@Tag(name = "Quản trị cấu hình", description = "Quy tắc đặt lịch / xếp ca do quản trị viên sửa, kèm lịch sử sửa")
@RestController
@RequestMapping("/api/booking/quan-tri/cau-hinh")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriCauHinhController {

    private final CauHinhService cauHinhService;

    @Operation(summary = "Quy tắc đang dùng, kèm phienBan (gửi lại khi lưu), lần sửa gần nhất và người sửa")
    @GetMapping
    public PhanHoiApi<CauHinhQuanTriResponse> xem() {
        return PhanHoiApi.ok(cauHinhService.layChoQuanTri());
    }

    @Operation(summary = "Lưu cả 10 quy tắc (chỉ áp dụng cho thao tác mới: ca, lịch hẹn, đánh giá đã có không đổi)."
            + " 400 nếu ngoài khoảng cho phép hoặc số lịch mỗi SĐT nhỏ hơn số lịch mỗi hồ sơ; 409 DU_LIEU_DA_THAY_DOI"
            + " nếu phienBan đã cũ. Không đổi gì thì không ghi lịch sử, phiên bản giữ nguyên")
    @PutMapping
    public PhanHoiApi<CauHinhQuanTriResponse> capNhat(@Valid @RequestBody CapNhatCauHinhRequest request) {
        return PhanHoiApi.ok(cauHinhService.capNhat(NguoiDungHienTai.layIdTaiKhoan(), request),
                "thanhCong.capNhatCauHinh");
    }

    @Operation(summary = "Lịch sử sửa cấu hình, mới nhất trước: mỗi trường đổi giá trị 1 dòng (cũ, mới, ai, lúc nào)")
    @GetMapping("/lich-su")
    public PhanHoiApi<TrangDuLieu<LichSuCauHinhResponse>> lichSu(
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(cauHinhService.lichSu(trang, kichThuoc));
    }

}
