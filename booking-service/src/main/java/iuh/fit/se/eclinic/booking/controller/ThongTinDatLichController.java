package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.NguoiThanDaLuuRequest;
import iuh.fit.se.eclinic.booking.dto.response.NguoiThanDaLuuResponse;
import iuh.fit.se.eclinic.booking.dto.response.ThongTinDatLichResponse;
import iuh.fit.se.eclinic.booking.service.LienKetHoSoService;
import iuh.fit.se.eclinic.booking.service.NguoiThanDaLuuService;
import iuh.fit.se.eclinic.booking.service.ThongTinDatLichService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * BOOK-02: thông tin điền sẵn form đặt lịch cho bệnh nhân đã đăng nhập. Luôn thao tác trên tài khoản của chính người
 * gọi. Người thân được lưu tự động khi đặt lịch (trừ khi request đặt lịch gửi {@code luuNguoiThan: false}).
 */
@Tag(name = "Thông tin đặt lịch đã lưu", description = "BOOK-02: điền sẵn form đặt lịch, người thân đã lưu")
@RestController
@RequestMapping("/api/booking/thong-tin-dat-lich/cua-toi")
@PreAuthorize("hasRole('BENH_NHAN')")
@RequiredArgsConstructor
public class ThongTinDatLichController {

    private final ThongTinDatLichService thongTinDatLichService;
    private final NguoiThanDaLuuService nguoiThanDaLuuService;
    private final LienKetHoSoService lienKetHoSoService;

    @Operation(summary = "Hồ sơ của tôi, người thân đã lưu, chuyên khoa và bác sĩ của lần đặt gần nhất")
    @GetMapping
    public PhanHoiApi<ThongTinDatLichResponse> cuaToi() {
        Long idTaiKhoan = NguoiDungHienTai.layIdTaiKhoan();
        lienKetHoSoService.thuLienKet(idTaiKhoan);
        return PhanHoiApi.ok(thongTinDatLichService.cuaToi(idTaiKhoan));
    }

    @Operation(summary = "Sửa thông tin đã lưu của 1 người thân (chỉ bản lưu của tài khoản, số CCCD không đổi được;"
            + " hồ sơ bệnh nhân và lịch hẹn đã đặt không bị ảnh hưởng)")
    @PutMapping("/nguoi-than/{id}")
    public PhanHoiApi<NguoiThanDaLuuResponse> suaNguoiThan(@PathVariable Long id,
            @Valid @RequestBody NguoiThanDaLuuRequest request) {
        return PhanHoiApi.ok(nguoiThanDaLuuService.sua(NguoiDungHienTai.layIdTaiKhoan(), id, request),
                "Đã cập nhật thông tin người thân");
    }

    @Operation(summary = "Bỏ 1 người thân khỏi danh sách đã lưu (lịch hẹn đã đặt không bị ảnh hưởng)")
    @DeleteMapping("/nguoi-than/{id}")
    public PhanHoiApi<Void> xoaNguoiThan(@PathVariable Long id) {
        nguoiThanDaLuuService.xoa(NguoiDungHienTai.layIdTaiKhoan(), id);
        return PhanHoiApi.ok(null, "Đã bỏ người thân khỏi danh sách đã lưu");
    }

}
