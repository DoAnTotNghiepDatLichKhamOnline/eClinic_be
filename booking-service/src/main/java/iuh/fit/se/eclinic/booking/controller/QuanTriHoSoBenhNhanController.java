package iuh.fit.se.eclinic.booking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.request.TuChoiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanDongResponse;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.HoSoChoXacMinhResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.service.LienKetHoSoService;
import iuh.fit.se.eclinic.booking.service.QuanLyHoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.service.SuaHoSoBenhNhanService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Quy tắc #3: quản trị viên xác minh hồ sơ bệnh nhân mà tài khoản khai đúng số CCCD nhưng thông tin không khớp. Duyệt
 * thì tài khoản xem được hồ sơ và lịch hẹn của hồ sơ; từ chối thì hồ sơ trở lại chưa liên kết.
 * <p>
 * Quản trị viên cũng xem / sửa hồ sơ bệnh nhân sau khi đối chiếu giấy tờ: việc đặt lịch không sửa hồ sơ đã có, nên hồ
 * sơ chưa liên kết tài khoản chỉ sửa được ở đây.
 * <p>
 * Màn hình Patient Management: danh sách / tìm kiếm hồ sơ (số CCCD đã che) và lịch hẹn của từng hồ sơ.
 */
@Tag(name = "Quản trị hồ sơ bệnh nhân", description = "Xác minh liên kết hồ sơ với tài khoản, xem / sửa hồ sơ bệnh nhân (quản trị viên)")
@RestController
@RequestMapping("/api/booking/quan-tri/ho-so-benh-nhan")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriHoSoBenhNhanController {

    private final LienKetHoSoService lienKetHoSoService;
    private final SuaHoSoBenhNhanService suaHoSoBenhNhanService;
    private final QuanLyHoSoBenhNhanService quanLyHoSoBenhNhanService;

    @Operation(summary = "Danh sách hồ sơ bệnh nhân (phân trang, hồ sơ mới tạo trước, số CCCD chỉ còn 4 số cuối); từ khoá"
            + " so với họ tên, số điện thoại, số bảo hiểm y tế, hoặc đủ 12 số CCCD; lọc theo trạng thái liên kết")
    @GetMapping
    public PhanHoiApi<TrangDuLieu<HoSoBenhNhanDongResponse>> danhSach(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) TrangThaiLienKet trangThaiLienKet,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanLyHoSoBenhNhanService.danhSach(tuKhoa, trangThaiLienKet, trang, kichThuoc));
    }

    @Operation(summary = "Lịch hẹn của 1 hồ sơ bệnh nhân (phân trang, mọi trạng thái, giờ khám muộn nhất trước), không có"
            + " chẩn đoán / đơn thuốc")
    @GetMapping("/{id}/lich-hen")
    public PhanHoiApi<TrangDuLieu<LichHenTrongCaResponse>> lichHen(@PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanLyHoSoBenhNhanService.lichHen(id, trang, kichThuoc));
    }

    @Operation(summary = "Hồ sơ bệnh nhân đang chờ xác minh liên kết với tài khoản, cũ nhất trước, phân trang")
    @GetMapping("/cho-xac-minh")
    public PhanHoiApi<TrangDuLieu<HoSoChoXacMinhResponse>> choXacMinh(
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(lienKetHoSoService.choXacMinh(trang, kichThuoc));
    }

    @Operation(summary = "Duyệt: hồ sơ thuộc về tài khoản đang chờ (409 nếu hồ sơ không ở trạng thái chờ xác minh)")
    @PostMapping("/{id}/duyet")
    public PhanHoiApi<Void> duyet(@PathVariable Long id) {
        lienKetHoSoService.duyet(id);
        return PhanHoiApi.ok(null, "Đã liên kết hồ sơ bệnh nhân với tài khoản");
    }

    @Operation(summary = "Từ chối: hồ sơ trở lại chưa liên kết, tài khoản này không được tự liên kết lại hồ sơ đó")
    @PostMapping("/{id}/tu-choi")
    public PhanHoiApi<Void> tuChoi(@PathVariable Long id,
            @Valid @RequestBody(required = false) TuChoiLienKetRequest request) {
        lienKetHoSoService.tuChoi(id, request == null ? null : request.lyDo());
        return PhanHoiApi.ok(null, "Đã từ chối liên kết hồ sơ bệnh nhân");
    }

    @Operation(summary = "Xem 1 hồ sơ bệnh nhân (số CCCD không che), id lấy từ idHoSoBenhNhan của lịch hẹn")
    @GetMapping("/{id}")
    public PhanHoiApi<HoSoBenhNhanQuanTriResponse> xem(@PathVariable Long id) {
        return PhanHoiApi.ok(suaHoSoBenhNhanService.xem(id));
    }

    @Operation(summary = "Sửa hồ sơ bệnh nhân sau khi đối chiếu giấy tờ; số CCCD chỉ điền được khi hồ sơ chưa có"
            + " (400 nếu đổi số đã có, 409 nếu số đó đã thuộc hồ sơ khác)")
    @PutMapping("/{id}")
    public PhanHoiApi<HoSoBenhNhanQuanTriResponse> sua(@PathVariable Long id,
            @Valid @RequestBody SuaHoSoBenhNhanRequest request) {
        return PhanHoiApi.ok(suaHoSoBenhNhanService.sua(id, request, NguoiDungHienTai.layIdTaiKhoan()),
                "Đã cập nhật hồ sơ bệnh nhân");
    }

}
