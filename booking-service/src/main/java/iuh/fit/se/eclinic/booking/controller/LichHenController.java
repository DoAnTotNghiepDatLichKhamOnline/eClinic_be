package iuh.fit.se.eclinic.booking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.DatLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.LocLichHen;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenCuaToiResponse;
import iuh.fit.se.eclinic.booking.service.DatLichService;
import iuh.fit.se.eclinic.booking.service.GioiHanDatLichService;
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.booking.service.LienKetHoSoService;
import iuh.fit.se.eclinic.booking.util.DiaChiIp;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * BOOK-01, BOOK-02, BOOK-11: đặt lịch khám. Công khai (app.bao-mat.duong-dan-cong-khai): khách không cần đăng nhập, bệnh
 * nhân đã đăng nhập gửi kèm token thì lịch được lưu vào tài khoản; số lần gọi từ 1 địa chỉ IP bị giới hạn
 * (GioiHanDatLichService). BOOK-07: lịch hẹn của tài khoản đang đăng nhập.
 */
@Tag(name = "Lịch hẹn", description = "BOOK-01..04, BOOK-07, BOOK-11: đặt lịch khám, lịch hẹn của tôi")
@RestController
@RequestMapping("/api/booking/lich-hen")
@RequiredArgsConstructor
public class LichHenController {

    private final DatLichService datLichService;
    private final GioiHanDatLichService gioiHanDatLichService;
    private final LichHenService lichHenService;
    private final LienKetHoSoService lienKetHoSoService;

    @Operation(summary = "Đặt lịch khám: khách không cần đăng nhập, bệnh nhân đã đăng nhập thì lịch lưu vào tài khoản;"
            + " người khám dưới 18 tuổi phải kèm người giám hộ")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<DatLichResponse> datLich(@Valid @RequestBody DatLichRequest request,
            HttpServletRequest httpRequest) {
        Long idTaiKhoan = null;
        if (NguoiDungHienTai.daDangNhap()) {
            if (NguoiDungHienTai.layVaiTro() != VaiTro.BENH_NHAN) {
                throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN,
                        "Tài khoản bác sĩ, quản trị viên không đặt được lịch khám; hãy đăng xuất để đặt như khách");
            }
            idTaiKhoan = NguoiDungHienTai.layIdTaiKhoan();
        }
        gioiHanDatLichService.ghiNhan(DiaChiIp.cua(httpRequest));
        return PhanHoiApi.ok(datLichService.datLich(request, idTaiKhoan), "Đặt lịch thành công");
    }

    @Operation(summary = "Lịch hẹn của tài khoản đang đăng nhập: lịch tài khoản đặt (cho bản thân hoặc người thân) và"
            + " lịch của hồ sơ bệnh nhân đã liên kết (kể cả lịch đặt như khách); lọc Tất cả / Sắp tới / Lịch sử, phân trang")
    @GetMapping("/cua-toi")
    @PreAuthorize("hasRole('BENH_NHAN')")
    public PhanHoiApi<TrangDuLieu<LichHenCuaToiResponse>> lichHenCuaToi(
            @RequestParam(defaultValue = "TAT_CA") LocLichHen loc,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        Long idTaiKhoan = NguoiDungHienTai.layIdTaiKhoan();
        // Lịch đặt như khách bằng số CCCD đã khai khi đăng ký phải có trong danh sách ngay lần xem đầu tiên (quy tắc #3)
        lienKetHoSoService.thuLienKet(idTaiKhoan);
        return PhanHoiApi.ok(lichHenService.lichHenCuaToi(idTaiKhoan, loc, trang, kichThuoc));
    }

}
