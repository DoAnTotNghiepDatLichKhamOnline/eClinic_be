package iuh.fit.se.eclinic.catalog.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoCuaToiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.ChuThichAnhRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.service.HoSoCuaBacSiService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * Bác sĩ đang đăng nhập xem hồ sơ giới thiệu của mình và tự sửa phần được phép (DOANTOTNGH-10 B7), có hiệu lực ngay:
 * giới thiệu ngắn, tiểu sử, ngôn ngữ khám, giải thưởng, công trình nghiên cứu (cả bản tiếng Anh) và ảnh làm việc. Học
 * vị, chức vụ, giấy phép, đào tạo, công tác, lĩnh vực khám chữa, năm bắt đầu hành nghề, ảnh chứng chỉ: quản trị viên sửa
 * ({@link QuanTriBacSiController}). Ảnh đại diện, số điện thoại: /api/users/me.
 * <p>
 * GET /api/catalog/bac-si/** là đường dẫn công khai nhưng @PreAuthorize vẫn chặn: không đăng nhập thì 401.
 */
@Tag(name = "Hồ sơ của bác sĩ", description = "Bác sĩ xem và tự sửa 1 phần hồ sơ giới thiệu, ảnh làm việc")
@RestController
@RequestMapping("/api/catalog/bac-si/toi")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class HoSoCuaBacSiController {

    private final HoSoCuaBacSiService hoSoCuaBacSiService;

    @Operation(summary = "Xem hồ sơ giới thiệu của tôi (mọi mục, cả 2 ngôn ngữ, kèm phienBanHoSo để gửi lại khi lưu)")
    @GetMapping("/ho-so")
    public PhanHoiApi<HoSoBacSiQuanTriResponse> layHoSo() {
        return PhanHoiApi.ok(hoSoCuaBacSiService.layHoSo(NguoiDungHienTai.layIdTaiKhoan()));
    }

    @Operation(summary = "Sửa phần hồ sơ của tôi: giới thiệu ngắn, tiểu sử, ngôn ngữ khám, giải thưởng, công trình nghiên"
            + " cứu (ghi đè các mục này, bỏ trống là xoá; mục khác giữ nguyên). 409 DU_LIEU_DA_THAY_DOI nếu phienBan đã cũ")
    @PutMapping("/ho-so")
    public PhanHoiApi<HoSoBacSiQuanTriResponse> capNhatHoSo(@Valid @RequestBody CapNhatHoSoCuaToiRequest request) {
        return PhanHoiApi.ok(hoSoCuaBacSiService.capNhatHoSo(NguoiDungHienTai.layIdTaiKhoan(), request),
                "thanhCong.capNhatHoSoCuaToi");
    }

    @Operation(summary = "Thêm ảnh làm việc (multipart: phần tệp 'anh' JPEG / PNG / WebP tối đa 4MB, 'chuThich',"
            + " 'chuThichEn'); luôn là ANH_CONG_VIEC")
    @PostMapping(path = "/anh", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<AnhBacSiQuanTriResponse> themAnh(@RequestPart("anh") MultipartFile anh,
            @RequestParam(required = false) @Size(max = 200, message = "{kiemTra.chuThich.toiDa}") String chuThich,
            @RequestParam(required = false) @Size(max = 200, message = "{kiemTra.chuThichEn.toiDa}") String chuThichEn)
            throws IOException {
        return PhanHoiApi.ok(hoSoCuaBacSiService.themAnh(NguoiDungHienTai.layIdTaiKhoan(), chuThich, chuThichEn,
                anh.getBytes()), "thanhCong.themAnhBacSi");
    }

    @Operation(summary = "Sửa chú thích ảnh làm việc của tôi (ảnh chứng chỉ: 403)")
    @PutMapping("/anh/{idAnh}")
    public PhanHoiApi<AnhBacSiQuanTriResponse> capNhatAnh(@PathVariable Long idAnh,
            @Valid @RequestBody ChuThichAnhRequest request) {
        return PhanHoiApi.ok(hoSoCuaBacSiService.capNhatAnh(NguoiDungHienTai.layIdTaiKhoan(), idAnh,
                request.chuThich(), request.chuThichEn()), "thanhCong.capNhatAnhBacSi");
    }

    @Operation(summary = "Xoá ảnh làm việc của tôi (ảnh chứng chỉ: 403)")
    @DeleteMapping("/anh/{idAnh}")
    public PhanHoiApi<Void> xoaAnh(@PathVariable Long idAnh) {
        hoSoCuaBacSiService.xoaAnh(NguoiDungHienTai.layIdTaiKhoan(), idAnh);
        return PhanHoiApi.ok(null, "thanhCong.xoaAnhBacSi");
    }

}
