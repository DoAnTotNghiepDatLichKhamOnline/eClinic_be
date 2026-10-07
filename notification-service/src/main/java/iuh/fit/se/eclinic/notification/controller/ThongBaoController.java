package iuh.fit.se.eclinic.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import iuh.fit.se.eclinic.notification.dto.response.DanhDauDaDocResponse;
import iuh.fit.se.eclinic.notification.dto.response.SoChuaDocResponse;
import iuh.fit.se.eclinic.notification.dto.response.ThongBaoResponse;
import iuh.fit.se.eclinic.notification.service.ThongBaoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * NOTI-01: chuông thông báo của tài khoản đang đăng nhập (mọi vai trò). Không nhận id tài khoản: lấy từ JWT. Không có
 * đẩy realtime: frontend hỏi lại {@code /so-chua-doc} định kỳ (khoảng 30 giây, tạm dừng khi tab bị ẩn).
 */
@Tag(name = "Thông báo", description = "NOTI-01: danh sách, số chưa đọc, đánh dấu đã đọc")
@RestController
@RequestMapping("/api/notification/thong-bao")
@RequiredArgsConstructor
public class ThongBaoController {

    private final ThongBaoService thongBaoService;

    @Operation(summary = "Thông báo của tôi, mới nhất trước; chuaDoc=true chỉ lấy thông báo chưa đọc")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<ThongBaoResponse>> danhSach(
            @RequestParam(defaultValue = "false") boolean chuaDoc,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(thongBaoService.danhSach(NguoiDungHienTai.layIdTaiKhoan(), NguoiDungHienTai.layVaiTro(),
                chuaDoc, trang, kichThuoc));
    }

    @Operation(summary = "Số thông báo chưa đọc của tôi (số trên biểu tượng chuông)")
    @GetMapping("/so-chua-doc")
    public PhanHoiApi<SoChuaDocResponse> soChuaDoc() {
        return PhanHoiApi.ok(thongBaoService.demChuaDoc(NguoiDungHienTai.layIdTaiKhoan()));
    }

    @Operation(summary = "Đánh dấu 1 thông báo của tôi đã đọc (gọi lại nhiều lần vẫn được)")
    @PostMapping("/{id}/da-doc")
    public PhanHoiApi<ThongBaoResponse> danhDauDaDoc(@PathVariable Long id) {
        return PhanHoiApi.ok(thongBaoService.danhDauDaDoc(NguoiDungHienTai.layIdTaiKhoan(),
                NguoiDungHienTai.layVaiTro(), id));
    }

    @Operation(summary = "Đánh dấu mọi thông báo của tôi đã đọc")
    @PostMapping("/da-doc-tat-ca")
    public PhanHoiApi<DanhDauDaDocResponse> danhDauDaDocTatCa() {
        return PhanHoiApi.ok(thongBaoService.danhDauDaDocTatCa(NguoiDungHienTai.layIdTaiKhoan()));
    }

}
