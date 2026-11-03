package iuh.fit.se.eclinic.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.GuiYeuCauDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.XinNghiNhieuCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.XuLyYeuCauRequest;
import iuh.fit.se.eclinic.booking.dto.response.YeuCauDoiLichResponse;
import iuh.fit.se.eclinic.booking.service.YeuCauDoiLichService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Bác sĩ xin đổi ca, xin nghỉ, nhường ca của chính mình và xem, rút yêu cầu đã gửi. Bác sĩ được nhường ca xem, đồng ý
 * hoặc từ chối yêu cầu gửi cho mình.
 */
@RestController
@RequestMapping("/api/booking/bac-si/toi")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class YeuCauDoiLichCuaBacSiController {

    private static final String TAG_DOI_LICH = "Yêu cầu đổi lịch của bác sĩ";
    private static final String MO_TA_DOI_LICH = "SCHED-02: bác sĩ xin đổi ca / xin nghỉ";
    private static final String TAG_NHUONG_CA = "Yêu cầu nhường ca gửi cho tôi";
    private static final String MO_TA_NHUONG_CA = "SCHED-03: bác sĩ nhận ca đồng ý / từ chối";

    private final YeuCauDoiLichService yeuCauDoiLichService;

    @Tag(name = TAG_DOI_LICH, description = MO_TA_DOI_LICH)
    @Operation(summary = "Gửi yêu cầu xin nghỉ (XIN_NGHI), đổi ca (DOI_CA, kèm ngày / giờ / phòng mong muốn) hoặc nhường ca"
            + " (NHUONG_CA, kèm idBacSiNhan cùng chuyên khoa; chờ bác sĩ nhận đồng ý rồi mới tới quản trị viên) cho 1 ca"
            + " của tôi, trước giờ bắt đầu ca ít nhất 24 giờ; mỗi ca chỉ có 1 yêu cầu còn chờ")
    @PostMapping("/yeu-cau-doi-lich")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<YeuCauDoiLichResponse> gui(@Valid @RequestBody GuiYeuCauDoiLichRequest request) {
        return PhanHoiApi.ok(yeuCauDoiLichService.gui(NguoiDungHienTai.layIdTaiKhoan(), request), "thanhCong.guiYeuCau");
    }

    @Tag(name = TAG_DOI_LICH, description = MO_TA_DOI_LICH)
    @Operation(summary = "Xin nghỉ nhiều ca của tôi trong 1 lần (tối đa 50): mỗi ca 1 yêu cầu XIN_NGHI chung mã nhóm. 1 ca"
            + " không gửi được thì không lưu gì và lỗi nêu ca đó")
    @PostMapping("/yeu-cau-doi-lich/xin-nghi-nhieu-ca")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<List<YeuCauDoiLichResponse>> xinNghiNhieuCa(@Valid @RequestBody XinNghiNhieuCaRequest request) {
        return PhanHoiApi.ok(yeuCauDoiLichService.xinNghiNhieuCa(NguoiDungHienTai.layIdTaiKhoan(), request),
                "thanhCong.xinNghiNhieuCa");
    }

    @Tag(name = TAG_DOI_LICH, description = MO_TA_DOI_LICH)
    @Operation(summary = "Các yêu cầu tôi đã gửi, mới nhất trước; lọc theo trạng thái")
    @GetMapping("/yeu-cau-doi-lich")
    public PhanHoiApi<TrangDuLieu<YeuCauDoiLichResponse>> cuaToi(
            @RequestParam(required = false) TrangThaiYeuCau trangThai,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(yeuCauDoiLichService.cuaToi(NguoiDungHienTai.layIdTaiKhoan(), trangThai, trang,
                kichThuoc));
    }

    @Tag(name = TAG_DOI_LICH, description = MO_TA_DOI_LICH)
    @Operation(summary = "Rút mọi yêu cầu còn chờ của 1 nhóm xin nghỉ nhiều ca của tôi")
    @PostMapping("/yeu-cau-doi-lich/nhom/{maNhom}/rut")
    public PhanHoiApi<List<YeuCauDoiLichResponse>> rutNhom(@PathVariable String maNhom) {
        return PhanHoiApi.ok(yeuCauDoiLichService.rutNhom(NguoiDungHienTai.layIdTaiKhoan(), maNhom), "thanhCong.rutNhom");
    }

    @Tag(name = TAG_DOI_LICH, description = MO_TA_DOI_LICH)
    @Operation(summary = "Rút 1 yêu cầu còn chờ (đồng nghiệp hoặc quản trị viên) của tôi (thành DA_RUT); sau đó ca nhận"
            + " được yêu cầu mới")
    @PostMapping("/yeu-cau-doi-lich/{id}/rut")
    public PhanHoiApi<YeuCauDoiLichResponse> rut(@PathVariable Long id) {
        return PhanHoiApi.ok(yeuCauDoiLichService.rut(NguoiDungHienTai.layIdTaiKhoan(), id), "thanhCong.rutYeuCau");
    }

    @Tag(name = TAG_NHUONG_CA, description = MO_TA_NHUONG_CA)
    @Operation(summary = "Yêu cầu nhường ca gửi cho tôi, mới nhất trước; lọc theo trạng thái (CHO_DONG_Y = đang chờ tôi)")
    @GetMapping("/yeu-cau-nhuong-ca")
    public PhanHoiApi<TrangDuLieu<YeuCauDoiLichResponse>> guiChoToi(
            @RequestParam(required = false) TrangThaiYeuCau trangThai,
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(yeuCauDoiLichService.guiChoToi(NguoiDungHienTai.layIdTaiKhoan(), trangThai, trang,
                kichThuoc));
    }

    @Tag(name = TAG_NHUONG_CA, description = MO_TA_NHUONG_CA)
    @Operation(summary = "Đồng ý nhận ca: yêu cầu thành CHO_DUYET và chờ quản trị viên duyệt. Tôi trùng giờ / vượt giờ làm"
            + " trong tuần -> 409 TRUNG_LICH_LAM_VIEC / VUOT_GIO_LAM_TUAN")
    @PostMapping("/yeu-cau-nhuong-ca/{id}/dong-y")
    public PhanHoiApi<YeuCauDoiLichResponse> dongY(@PathVariable Long id) {
        return PhanHoiApi.ok(yeuCauDoiLichService.dongY(NguoiDungHienTai.layIdTaiKhoan(), id), "thanhCong.dongYNhanCa");
    }

    @Tag(name = TAG_NHUONG_CA, description = MO_TA_NHUONG_CA)
    @Operation(summary = "Từ chối nhận ca (ghi chú tùy chọn): yêu cầu thành DONG_NGHIEP_TU_CHOI")
    @PostMapping("/yeu-cau-nhuong-ca/{id}/tu-choi")
    public PhanHoiApi<YeuCauDoiLichResponse> tuChoi(@PathVariable Long id,
            @Valid @RequestBody(required = false) XuLyYeuCauRequest request) {
        return PhanHoiApi.ok(yeuCauDoiLichService.tuChoiNhan(NguoiDungHienTai.layIdTaiKhoan(), id,
                request == null ? null : request.ghiChu()), "thanhCong.tuChoiNhanCa");
    }

}
