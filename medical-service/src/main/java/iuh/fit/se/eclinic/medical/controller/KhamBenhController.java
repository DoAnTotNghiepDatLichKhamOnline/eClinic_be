package iuh.fit.se.eclinic.medical.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import iuh.fit.se.eclinic.medical.dto.response.Icd10Response;
import iuh.fit.se.eclinic.medical.dto.response.LichSuSuaBenhAnResponse;
import iuh.fit.se.eclinic.medical.dto.response.ThuocGoiYResponse;
import iuh.fit.se.eclinic.medical.mapper.BenhAnMapper;
import iuh.fit.se.eclinic.medical.service.Icd10Service;
import iuh.fit.se.eclinic.medical.service.KhamBenhService;
import iuh.fit.se.eclinic.medical.service.LichSuSuaBenhAnService;
import iuh.fit.se.eclinic.medical.service.ThuocService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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
    private final Icd10Service icd10Service;
    private final LichSuSuaBenhAnService lichSuSuaBenhAnService;
    private final BenhAnMapper benhAnMapper;

    @Operation(summary = "Ghi nhận kết quả khám: lưu hồ sơ bệnh án kèm đơn thuốc và chuyển lịch hẹn sang đã hoàn thành"
            + " (409 nếu chưa đến ngày khám, hoặc lịch hẹn đã khám xong / đã hủy). Thuốc chọn theo idThuoc trong danh mục: 404"
            + " nếu không có, 409 THUOC_NGUNG_DUNG / THUOC_CHUA_XAC_MINH, 400 nếu 1 thuốc có 2 dòng. 400 nếu mã ICD-10 không"
            + " có trong danh mục, huyết áp thiếu 1 số hoặc tâm trương >= tâm thu, 1 thuốc quá 30 ngày (90 khi chẩn đoán chính"
            + " mạn tính), hoặc trẻ dưới 72 tháng có kê thuốc mà thiếu cân nặng. Phản hồi kèm maSoKham và linkSoKham (sổ khám"
            + " bệnh của bệnh nhân, EXAM-03) để đưa link hoặc mở mã QR cho bệnh nhân")
    @PostMapping("/lich-hen/{idLichHen}/benh-an")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<BenhAnResponse> ghiNhan(@PathVariable Long idLichHen,
            @Valid @RequestBody BenhAnRequest request) {
        return PhanHoiApi.ok(khamBenhService.ghiNhan(NguoiDungHienTai.layIdTaiKhoan(), idLichHen, request),
                "thanhCong.ghiNhanKetQuaKham");
    }

    @Operation(summary = "Sửa hồ sơ bệnh án đã ghi: thay chẩn đoán, ghi chú, ngày tái khám và toàn bộ đơn thuốc (thuốc đã có"
            + " trong đơn giữ lại được dù đã ngừng dùng / chưa xác minh). Bắt buộc lyDoSua và phienBan đang xem; mỗi lần sửa"
            + " thêm 1 dòng lịch sử sửa. 400 nếu thiếu lý do / phiên bản hoặc nội dung không đổi gì; 409 DU_LIEU_DA_THAY_DOI nếu"
            + " đã có người sửa trước (tải lại hồ sơ), 409 HET_HAN_SUA_BENH_AN nếu đã quá hạn sửa (mặc định 7 ngày từ lúc ghi"
            + " nhận)")
    @PutMapping("/lich-hen/{idLichHen}/benh-an")
    public PhanHoiApi<BenhAnResponse> sua(@PathVariable Long idLichHen,
            @Validated({ Default.class, BenhAnRequest.KhiSua.class }) @RequestBody BenhAnRequest request) {
        return PhanHoiApi.ok(khamBenhService.sua(NguoiDungHienTai.layIdTaiKhoan(), idLichHen, request),
                "thanhCong.capNhatBenhAn");
    }

    @Operation(summary = "Hồ sơ bệnh án của 1 lịch hẹn của tôi, kèm maSoKham và linkSoKham của sổ khám bệnh (404 nếu chưa"
            + " ghi nhận kết quả khám)")
    @GetMapping("/lich-hen/{idLichHen}/benh-an")
    public PhanHoiApi<BenhAnResponse> xem(@PathVariable Long idLichHen) {
        return PhanHoiApi.ok(khamBenhService.xem(NguoiDungHienTai.layIdTaiKhoan(), idLichHen));
    }

    @Operation(summary = "Lịch sử sửa hồ sơ bệnh án của 1 lượt khám (bác sĩ, thời điểm, lý do, phần đã đổi, nội dung trước /"
            + " sau; mới nhất trước). Xem được lượt khám của mình và lượt khám của bệnh nhân mà mình có lịch hẹn từ giờ khám đó"
            + " trở đi (như lần khám trước trong hồ sơ khám); không thì 404")
    @GetMapping("/lich-hen/{idLichHen}/benh-an/lich-su")
    public PhanHoiApi<LichSuSuaBenhAnResponse> lichSuSua(@PathVariable Long idLichHen) {
        return PhanHoiApi.ok(lichSuSuaBenhAnService.cuaBacSi(NguoiDungHienTai.layIdTaiKhoan(), idLichHen));
    }

    @Operation(summary = "Gợi ý thuốc kê được (đang dùng, đã xác minh) theo một phần tên hoặc hoạt chất (tối đa 20; từ"
            + " khoá trống trả danh sách rỗng)")
    @GetMapping("/thuoc")
    public PhanHoiApi<List<ThuocGoiYResponse>> goiYThuoc(@RequestParam(defaultValue = "") String tuKhoa) {
        return PhanHoiApi.ok(thuocService.timKiem(tuKhoa).stream().map(benhAnMapper::toGoiY).toList());
    }

    @Operation(summary = "Gợi ý mã ICD-10 cho chẩn đoán chính theo đầu mã hoặc một phần tên tiếng Việt / tiếng Anh (tối đa"
            + " 20, theo mã; từ khoá trống trả danh sách rỗng). manTinh = bệnh thuộc Phụ lục VII Thông tư 26/2025: thuốc kê"
            + " được tối đa 90 ngày thay vì 30")
    @GetMapping("/icd10")
    public PhanHoiApi<List<Icd10Response>> goiYIcd10(@RequestParam(defaultValue = "") String tuKhoa) {
        return PhanHoiApi.ok(icd10Service.timKiem(tuKhoa).stream().map(benhAnMapper::toIcd10).toList());
    }

}
