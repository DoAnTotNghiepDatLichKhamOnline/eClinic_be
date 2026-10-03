package iuh.fit.se.eclinic.identity.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.service.AnhDaiDienService;
import lombok.RequiredArgsConstructor;

/**
 * Ảnh đại diện của người đang đăng nhập. Mọi vai trò đều gọi được, luôn thao tác trên tài khoản của chính người gọi.
 * <p>
 * Tải lên bằng multipart/form-data, phần tệp tên {@code anh}: JPEG, PNG hoặc WebP, tối đa 2MB
 * (spring.servlet.multipart.max-file-size). Tên tệp và Content-Type của phần tệp không được dùng để kiểm tra.
 */
@Tag(name = "Ảnh đại diện", description = "Đổi và bỏ ảnh đại diện của người đang đăng nhập")
@RestController
@RequestMapping("/api/users/me/avatar")
@RequiredArgsConstructor
public class AnhDaiDienController {

    private final AnhDaiDienService anhDaiDienService;

    @Operation(summary = "Đổi ảnh đại diện (multipart, phần tệp 'anh': JPEG / PNG / WebP, tối đa 2MB); trả về hồ sơ với ảnh mới")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhanHoiApi<HoSoCaNhanResponse> taiLen(@RequestPart("anh") MultipartFile anh) throws IOException {
        return PhanHoiApi.ok(anhDaiDienService.taiLen(NguoiDungHienTai.layIdTaiKhoan(), anh.getBytes()),
                "Đã đổi ảnh đại diện");
    }

    @Operation(summary = "Bỏ ảnh đại diện (kể cả ảnh lấy từ Google); chưa có ảnh thì không làm gì")
    @DeleteMapping
    public PhanHoiApi<HoSoCaNhanResponse> xoa() {
        return PhanHoiApi.ok(anhDaiDienService.xoa(NguoiDungHienTai.layIdTaiKhoan()), "Đã bỏ ảnh đại diện");
    }

}
