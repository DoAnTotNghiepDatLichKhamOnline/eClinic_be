package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.HoSoCuaToiRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoCuaToiResponse;
import iuh.fit.se.eclinic.booking.service.HoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.service.LienKetHoSoService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * PAT-01: bệnh nhân xem, tạo, sửa hồ sơ bệnh nhân của tài khoản mình. Form đặt lịch "cho bản thân" lấy dữ liệu điền sẵn
 * từ đây.
 */
@Tag(name = "Hồ sơ bệnh nhân", description = "PAT-01: hồ sơ bệnh nhân của tài khoản đang đăng nhập")
@RestController
@RequestMapping("/api/booking/ho-so-benh-nhan")
@PreAuthorize("hasRole('BENH_NHAN')")
@RequiredArgsConstructor
public class HoSoBenhNhanController {

    private final HoSoBenhNhanService hoSoBenhNhanService;
    private final LienKetHoSoService lienKetHoSoService;

    @Operation(summary = "Xem hồ sơ bệnh nhân của tôi (404 nếu tài khoản chưa có hồ sơ)")
    @GetMapping("/cua-toi")
    public PhanHoiApi<HoSoCuaToiResponse> xemCuaToi() {
        Long idTaiKhoan = NguoiDungHienTai.layIdTaiKhoan();
        lienKetHoSoService.thuLienKet(idTaiKhoan);
        return PhanHoiApi.ok(hoSoBenhNhanService.xemCuaToi(idTaiKhoan));
    }

    @Operation(summary = "Tạo hồ sơ bệnh nhân của tôi (khi chưa có) hoặc sửa thông tin; số CCCD không đổi được. Số CCCD"
            + " đã có hồ sơ (đặt lịch như khách): khớp họ tên và ngày sinh hoặc SĐT thì liên kết ngay, không khớp thì trả"
            + " trangThaiLienKet CHO_XAC_MINH")
    @PutMapping("/cua-toi")
    public PhanHoiApi<HoSoCuaToiResponse> luuCuaToi(@Valid @RequestBody HoSoCuaToiRequest request) {
        return PhanHoiApi.ok(hoSoBenhNhanService.luuCuaToi(NguoiDungHienTai.layIdTaiKhoan(), request),
                "Đã lưu hồ sơ bệnh nhân");
    }

}
