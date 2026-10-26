package iuh.fit.se.eclinic.report.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.report.dto.request.KyBaoCao;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse;
import iuh.fit.se.eclinic.report.service.ExcelBaoCaoService;
import iuh.fit.se.eclinic.report.service.ThongKeService;
import iuh.fit.se.eclinic.report.util.TepTaiVe;
import lombok.RequiredArgsConstructor;

/** Báo cáo thống kê của quản trị viên: xem (JSON cho màn hình biểu đồ) và tải về Excel, cùng bộ lọc. */
@Tag(name = "Báo cáo thống kê", description = "DASH-01..04, ADM-05: số liệu theo kỳ ngày / tuần / tháng / năm / tuỳ"
        + " chọn, theo chuyên khoa, bác sĩ, thời gian; xuất Excel")
@RestController
@RequestMapping("/api/report/quan-tri/thong-ke")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class ThongKeController {

    private final ThongKeService thongKeService;
    private final ExcelBaoCaoService excelBaoCaoService;

    @Operation(summary = "Báo cáo thống kê: tổng quan (bác sĩ, bệnh nhân duy nhất / lượt khám, lượt đặt mới, tỷ lệ hủy,"
            + " không đến, lấp đầy lượt khám), trạng thái, kênh đặt, thời gian đặt trước, theo chuyên khoa, theo bác sĩ,"
            + " chuỗi theo thời gian. Kỳ: NGAY / TUAN / THANG (mặc định) / NAM chứa ngày mốc (mặc định hôm nay), hoặc"
            + " TUY_CHON từ ngày - đến ngày (tối đa 366 ngày)")
    @GetMapping
    public PhanHoiApi<BaoCaoThongKeResponse> xem(
            @RequestParam(required = false) KyBaoCao ky,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idChuyenKhoa) {
        return PhanHoiApi.ok(thongKeService.lap(ky, ngay, tuNgay, denNgay, idChuyenKhoa));
    }

    // Không khai báo produces: lỗi (kỳ sai...) vẫn trả JSON như các API khác
    @Operation(summary = "Tải báo cáo thống kê dạng Excel (cùng bộ lọc): trang tổng quan, theo chuyên khoa, theo bác sĩ,"
            + " theo thời gian và trang ghi chú cách tính")
    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> xuatExcel(
            @RequestParam(required = false) KyBaoCao ky,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idChuyenKhoa) {
        BaoCaoThongKeResponse baoCao = thongKeService.lap(ky, ngay, tuNgay, denNgay, idChuyenKhoa);
        return TepTaiVe.taiVe(excelBaoCaoService.xuat(baoCao),
                "bao-cao-thong-ke_" + baoCao.tuNgay() + "_" + baoCao.denNgay() + ".xlsx", TepTaiVe.XLSX);
    }

}
