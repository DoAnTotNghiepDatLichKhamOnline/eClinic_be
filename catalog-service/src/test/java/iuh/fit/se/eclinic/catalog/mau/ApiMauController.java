package iuh.fit.se.eclinic.catalog.mau;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Controller mẫu CHỈ có trong test, dùng để kiểm tra khung dùng chung (KhungDungChungTest).
 */
@RestController
@RequestMapping("/api/mau")
public class ApiMauController {

    public record YeuCauMau(@NotBlank String hoTen, @NotBlank String email) {
    }

    @GetMapping("/cong-khai")
    public PhanHoiApi<LocalDateTime> congKhai() {
        return PhanHoiApi.ok(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    @GetMapping("/can-dang-nhap")
    public PhanHoiApi<Long> canDangNhap() {
        return PhanHoiApi.ok(NguoiDungHienTai.layIdTaiKhoan());
    }

    @GetMapping("/chi-admin")
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public PhanHoiApi<String> chiAdmin() {
        return PhanHoiApi.ok("xin chào admin");
    }

    @PostMapping("/kiem-tra")
    public PhanHoiApi<YeuCauMau> kiemTra(@Valid @RequestBody YeuCauMau yeuCau) {
        return PhanHoiApi.ok(yeuCau, "Hợp lệ");
    }

    @GetMapping("/loi-nghiep-vu")
    public PhanHoiApi<Void> loiNghiepVu() {
        throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC);
    }

    @GetMapping("/khong-tim-thay")
    public PhanHoiApi<Void> khongTimThay() {
        throw new LoiKhongTimThay("ChuyenKhoa", 999);
    }

}
