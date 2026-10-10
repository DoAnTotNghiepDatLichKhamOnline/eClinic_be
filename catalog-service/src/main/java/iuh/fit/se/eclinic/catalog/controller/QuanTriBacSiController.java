package iuh.fit.se.eclinic.catalog.controller;

import java.io.IOException;
import java.util.List;

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
import iuh.fit.se.eclinic.catalog.dto.request.CapNhatAnhBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.NgungCongTacRequest;
import iuh.fit.se.eclinic.catalog.dto.request.SapXepAnhBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.SuaBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.ThemBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiResponse;
import iuh.fit.se.eclinic.catalog.dto.response.AnhHuongNgungCongTacResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.KetQuaNgungCongTacResponse;
import iuh.fit.se.eclinic.catalog.service.AnhBacSiService;
import iuh.fit.se.eclinic.catalog.service.HoSoBacSiService;
import iuh.fit.se.eclinic.catalog.service.QuanLyBacSiService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * DOC-01: quản trị viên sửa hồ sơ giới thiệu và ảnh giới thiệu của bác sĩ. Tách khỏi {@link BacSiController} (công khai)
 * vì quản trị viên xem / sửa được cả bác sĩ không hiển thị công khai (ngừng công tác, tài khoản bị vô hiệu hoá).
 * <p>
 * Ảnh tải lên bằng multipart/form-data, phần tệp tên {@code anh}: JPEG, PNG hoặc WebP, tối đa 4MB
 * (spring.servlet.multipart.max-file-size). Tên tệp và Content-Type của phần tệp không được dùng để kiểm tra.
 */
@Tag(name = "Quản trị bác sĩ", description = "DOC-01: hồ sơ giới thiệu và ảnh giới thiệu của bác sĩ (quản trị viên)")
@RestController
@RequestMapping("/api/catalog/quan-tri/bac-si")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriBacSiController {

    private final HoSoBacSiService hoSoBacSiService;
    private final AnhBacSiService anhBacSiService;
    private final QuanLyBacSiService quanLyBacSiService;

    @Operation(summary = "Danh bạ bác sĩ (phân trang, bác sĩ mới thêm đứng trước, kể cả bác sĩ ngừng công tác); lọc theo từ"
            + " khoá (họ tên, email, số điện thoại, số giấy phép), chuyên khoa, trạng thái")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<BacSiQuanTriResponse>> danhSach(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) Long idChuyenKhoa,
            @RequestParam(required = false) TrangThaiBacSi trangThai,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanLyBacSiService.danhSach(tuKhoa, idChuyenKhoa, trangThai, trang, kichThuoc));
    }

    @Operation(summary = "Thêm bác sĩ: tạo tài khoản đăng nhập (mật khẩu mặc định, phải đặt mật khẩu ở lần đăng nhập đầu) và"
            + " hồ sơ bác sĩ trong 1 bước")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<HoSoBacSiQuanTriResponse> them(@Valid @RequestBody ThemBacSiRequest request) {
        return PhanHoiApi.ok(quanLyBacSiService.them(request), "Đã thêm bác sĩ");
    }

    @Operation(summary = "Sửa thông tin cơ bản: họ tên, số điện thoại, chuyên khoa, số giấy phép (đổi chuyên khoa khi còn ca"
            + " sắp tới: 409 BAC_SI_CON_CA_LAM_VIEC)")
    @PutMapping("/{id}")
    public PhanHoiApi<HoSoBacSiQuanTriResponse> sua(@PathVariable Long id, @Valid @RequestBody SuaBacSiRequest request) {
        return PhanHoiApi.ok(quanLyBacSiService.sua(id, request), "Đã cập nhật thông tin bác sĩ");
    }

    @Operation(summary = "Xem trước khi cho ngừng công tác: số ca sắp tới sẽ bị hủy và số lịch hẹn sẽ phải đổi lịch")
    @GetMapping("/{id}/anh-huong-ngung-cong-tac")
    public PhanHoiApi<AnhHuongNgungCongTacResponse> anhHuongNgungCongTac(@PathVariable Long id) {
        return PhanHoiApi.ok(quanLyBacSiService.anhHuongNgungCongTac(id));
    }

    @Operation(summary = "Cho bác sĩ ngừng công tác: hủy mọi ca sắp tới (lịch hẹn được giữ và đánh dấu cần đổi lịch, bệnh nhân"
            + " được báo), ẩn khỏi danh sách công khai, vô hiệu hoá tài khoản. Trả 503 DICH_VU_NOI_BO_LOI thì gửi lại")
    @PostMapping("/{id}/ngung-cong-tac")
    public PhanHoiApi<KetQuaNgungCongTacResponse> ngungCongTac(@PathVariable Long id,
            @Valid @RequestBody NgungCongTacRequest request) {
        return PhanHoiApi.ok(quanLyBacSiService.ngungCongTac(NguoiDungHienTai.layIdTaiKhoan(), id, request.lyDo()),
                "Bác sĩ đã ngừng công tác");
    }

    @Operation(summary = "Cho bác sĩ công tác lại: kích hoạt lại tài khoản, hiện lại trong danh sách công khai. Các ca đã hủy"
            + " không được khôi phục")
    @PostMapping("/{id}/cong-tac-lai")
    public PhanHoiApi<HoSoBacSiQuanTriResponse> congTacLai(@PathVariable Long id) {
        return PhanHoiApi.ok(quanLyBacSiService.congTacLai(id), "Bác sĩ đã công tác lại");
    }

    @Operation(summary = "Xem hồ sơ giới thiệu của bác sĩ (kể cả bác sĩ không hiển thị công khai)")
    @GetMapping("/{id}")
    public PhanHoiApi<HoSoBacSiQuanTriResponse> layHoSo(@PathVariable Long id) {
        return PhanHoiApi.ok(hoSoBacSiService.layHoSo(id));
    }

    @Operation(summary = "Sửa hồ sơ giới thiệu của bác sĩ (ghi đè mọi trường, trường bỏ trống bị xoá trắng)")
    @PutMapping("/{id}/ho-so")
    public PhanHoiApi<HoSoBacSiQuanTriResponse> capNhatHoSo(@PathVariable Long id,
            @Valid @RequestBody CapNhatHoSoBacSiRequest request) {
        return PhanHoiApi.ok(hoSoBacSiService.capNhatHoSo(id, request), "Đã cập nhật hồ sơ bác sĩ");
    }

    @Operation(summary = "Thêm ảnh giới thiệu (multipart: phần tệp 'anh' JPEG / PNG / WebP tối đa 4MB, 'loai', 'chuThich')")
    @PostMapping(path = "/{id}/anh", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<AnhBacSiResponse> themAnh(@PathVariable Long id,
            @RequestPart("anh") MultipartFile anh,
            @RequestParam LoaiAnhBacSi loai,
            @RequestParam(required = false) @Size(max = 200, message = "Chú thích tối đa 200 ký tự") String chuThich)
            throws IOException {
        return PhanHoiApi.ok(anhBacSiService.them(id, loai, chuThich, anh.getBytes()), "Đã thêm ảnh giới thiệu");
    }

    @Operation(summary = "Sửa loại và chú thích của ảnh giới thiệu")
    @PutMapping("/{id}/anh/{idAnh}")
    public PhanHoiApi<AnhBacSiResponse> capNhatAnh(@PathVariable Long id, @PathVariable Long idAnh,
            @Valid @RequestBody CapNhatAnhBacSiRequest request) {
        return PhanHoiApi.ok(anhBacSiService.capNhat(id, idAnh, request), "Đã cập nhật ảnh giới thiệu");
    }

    @Operation(summary = "Sắp xếp lại ảnh giới thiệu (gửi id của mọi ảnh theo thứ tự mới)")
    @PutMapping("/{id}/anh/thu-tu")
    public PhanHoiApi<List<AnhBacSiResponse>> sapXepAnh(@PathVariable Long id,
            @Valid @RequestBody SapXepAnhBacSiRequest request) {
        return PhanHoiApi.ok(anhBacSiService.sapXep(id, request.idAnh()), "Đã sắp xếp ảnh giới thiệu");
    }

    @Operation(summary = "Xoá ảnh giới thiệu")
    @DeleteMapping("/{id}/anh/{idAnh}")
    public PhanHoiApi<Void> xoaAnh(@PathVariable Long id, @PathVariable Long idAnh) {
        anhBacSiService.xoa(id, idAnh);
        return PhanHoiApi.ok(null, "Đã xoá ảnh giới thiệu");
    }

}
