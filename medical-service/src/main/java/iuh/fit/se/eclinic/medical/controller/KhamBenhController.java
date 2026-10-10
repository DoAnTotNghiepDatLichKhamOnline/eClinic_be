package iuh.fit.se.eclinic.medical.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import iuh.fit.se.eclinic.medical.dto.request.BenhAnRequest;
import iuh.fit.se.eclinic.medical.dto.response.BenhAnResponse;
import iuh.fit.se.eclinic.medical.dto.response.ThuocGoiYResponse;
import iuh.fit.se.eclinic.medical.mapper.BenhAnMapper;
import iuh.fit.se.eclinic.medical.service.KhamBenhService;
import iuh.fit.se.eclinic.medical.service.ThuocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EXAM-01, EXAM-02: bác sĩ ghi nhận kết quả khám cho lịch hẹn của CHÍNH MÌNH. Không nhận id bác sĩ: bác sĩ lấy từ tài
 * khoản đang đăng nhập; lịch hẹn của bác sĩ khác coi như không có (404).
 */
@Tag(name = "Khám bệnh", description = "EXAM-01, EXAM-02: bác sĩ ghi hồ sơ bệnh án, đơn thuốc cho lịch hẹn của mình")
@RestController
@RequestMapping("/api/medical/bac-si/toi")
@PreAuthorize("hasRole('BAC_SI')")
@RequiredArgsConstructor
public class KhamBenhController {

    private final KhamBenhService khamBenhService;
    private final ThuocService thuocService;
    private final BenhAnMapper benhAnMapper;

    @Operation(summary = "Ghi nhận kết quả khám: lưu hồ sơ bệnh án kèm đơn thuốc và chuyển lịch hẹn sang đã hoàn thành"
            + " (409 nếu chưa đến ngày khám, hoặc lịch hẹn đã khám xong / đã hủy)")
    @PostMapping("/lich-hen/{idLichHen}/benh-an")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<BenhAnResponse> ghiNhan(@PathVariable Long idLichHen,
            @Valid @RequestBody BenhAnRequest request) {
        return PhanHoiApi.ok(khamBenhService.ghiNhan(NguoiDungHienTai.layIdTaiKhoan(), idLichHen, request),
                "Đã ghi nhận kết quả khám");
    }

    @Operation(summary = "Sửa hồ sơ bệnh án đã ghi: thay chẩn đoán, ghi chú, ngày tái khám và toàn bộ đơn thuốc")
    @PutMapping("/lich-hen/{idLichHen}/benh-an")
    public PhanHoiApi<BenhAnResponse> sua(@PathVariable Long idLichHen, @Valid @RequestBody BenhAnRequest request) {
        return PhanHoiApi.ok(khamBenhService.sua(NguoiDungHienTai.layIdTaiKhoan(), idLichHen, request),
                "Đã cập nhật hồ sơ bệnh án");
    }

    @Operation(summary = "Hồ sơ bệnh án của 1 lịch hẹn của tôi (404 nếu chưa ghi nhận kết quả khám)")
    @GetMapping("/lich-hen/{idLichHen}/benh-an")
    public PhanHoiApi<BenhAnResponse> xem(@PathVariable Long idLichHen) {
        return PhanHoiApi.ok(khamBenhService.xem(NguoiDungHienTai.layIdTaiKhoan(), idLichHen));
    }

    @Operation(summary = "Gợi ý thuốc trong danh mục theo một phần tên (tối đa 20; từ khoá trống trả danh sách rỗng)")
    @GetMapping("/thuoc")
    public PhanHoiApi<List<ThuocGoiYResponse>> goiYThuoc(@RequestParam(defaultValue = "") String tuKhoa) {
        return PhanHoiApi.ok(thuocService.timKiem(tuKhoa).stream().map(benhAnMapper::toGoiY).toList());
    }

}
