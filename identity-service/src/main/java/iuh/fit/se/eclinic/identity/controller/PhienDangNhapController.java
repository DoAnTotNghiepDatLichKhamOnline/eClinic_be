package iuh.fit.se.eclinic.identity.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import iuh.fit.se.eclinic.identity.dto.response.PhienDangNhapResponse;
import iuh.fit.se.eclinic.identity.service.PhienDangNhapService;
import lombok.RequiredArgsConstructor;

/**
 * Các thiết bị đang đăng nhập của người gọi: xem danh sách, đăng xuất 1 thiết bị, đăng xuất mọi thiết bị khác.
 * Mọi vai trò đều gọi được. Thiết bị đang dùng được nhận ra qua claim "phien" của access token (refresh token chỉ đi
 * tới /api/auth nên không dùng được ở đây).
 * <p>
 * Thiết bị bị đăng xuất không làm mới phiên được nữa, nhưng access token nó đang giữ còn dùng được tới khi hết hạn
 * (tối đa 30 phút).
 */
@Tag(name = "Phiên đăng nhập", description = "Xem và đăng xuất các thiết bị đang đăng nhập của người đang đăng nhập")
@RestController
@RequestMapping("/api/users/me/sessions")
@RequiredArgsConstructor
public class PhienDangNhapController {

    private final PhienDangNhapService phienDangNhapService;

    @Operation(summary = "Danh sách thiết bị đang đăng nhập; thiết bị đang dùng đứng đầu (hienTai = true)")
    @GetMapping
    public PhanHoiApi<List<PhienDangNhapResponse>> layDanhSach() {
        return PhanHoiApi.ok(phienDangNhapService.layDanhSach(NguoiDungHienTai.layIdTaiKhoan(),
                NguoiDungHienTai.layMaPhien()));
    }

    @Operation(summary = "Đăng xuất 1 thiết bị khác theo id trong danh sách (thiết bị đang dùng thì gọi /api/auth/logout)")
    @DeleteMapping("/{id}")
    public PhanHoiApi<Void> dangXuat(@PathVariable String id) {
        phienDangNhapService.dangXuat(NguoiDungHienTai.layIdTaiKhoan(), NguoiDungHienTai.layMaPhien(), id);
        return PhanHoiApi.ok(null, "Đã đăng xuất thiết bị");
    }

    @Operation(summary = "Đăng xuất mọi thiết bị khác, giữ thiết bị đang dùng; trả về số thiết bị đã đăng xuất")
    @DeleteMapping
    public PhanHoiApi<Integer> dangXuatCacPhienKhac() {
        int soPhien = phienDangNhapService.dangXuatCacPhienKhac(NguoiDungHienTai.layIdTaiKhoan(),
                NguoiDungHienTai.layMaPhien());
        return PhanHoiApi.ok(soPhien, "Đã đăng xuất " + soPhien + " thiết bị khác");
    }

}
