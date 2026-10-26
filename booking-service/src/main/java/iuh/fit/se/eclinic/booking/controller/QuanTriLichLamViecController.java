package iuh.fit.se.eclinic.booking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import iuh.fit.se.eclinic.booking.dto.request.GuiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.HuyCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.SaoChepCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.SuaCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.TaoCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.GioLamTheoTuanResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaGuiLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaSaoChepResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaTaoCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichSuCaResponse;
import iuh.fit.se.eclinic.booking.service.ExcelLichLamViecService;
import iuh.fit.se.eclinic.booking.service.GuiLichLamViecService;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.PdfLichLamViecService;
import iuh.fit.se.eclinic.booking.service.PdfLichLamViecService.TepPdf;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.booking.util.TepTaiVe;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * ADM-04, SCHED-01/05/06: quản trị viên xem lịch làm việc toàn viện, giờ làm theo tuần của bác sĩ, lịch hẹn và lịch sử
 * thay đổi của từng ca; xếp / sửa / hủy ca, sao chép ca sang khoảng ngày khác, nhập ca từ tệp Excel (cả 2 đều xem trước
 * rồi mới lưu), xuất lịch ra Excel, in lịch ra PDF (của 1 bác sĩ, lưới tuần toàn viện) và gửi lịch cho bác sĩ (email +
 * thông báo); quy tắc ở {@link QuanLyCaService}, bố cục tệp ở {@link ExcelLichLamViecService} /
 * {@link PdfLichLamViecService}.
 * Ngày theo dạng yyyy-MM-dd.
 */
@Tag(name = "Quản trị lịch làm việc", description = "ADM-04, SCHED-01/05/06: lịch làm việc toàn viện, xếp / sửa / hủy ca,"
        + " sao chép ca, xuất / nhập Excel, in PDF, gửi lịch cho bác sĩ, lịch hẹn của ca (quản trị viên)")
@RestController
@RequestMapping("/api/booking/quan-tri/lich-lam-viec")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriLichLamViecController {

    private final LichLamViecService lichLamViecService;
    private final QuanLyCaService quanLyCaService;
    private final ExcelLichLamViecService excelLichLamViecService;
    private final PdfLichLamViecService pdfLichLamViecService;
    private final GuiLichLamViecService guiLichLamViecService;

    @Operation(summary = "Các ca làm việc trong khoảng ngày (tối đa 42 ngày, kể cả ca đã huỷ) kèm số lượt đã đặt;"
            + " lọc theo chuyên khoa, bác sĩ, phòng khám")
    @GetMapping
    public PhanHoiApi<List<CaLamViecResponse>> lichToanVien(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idChuyenKhoa,
            @RequestParam(required = false) Long idBacSi,
            @RequestParam(required = false) Long idPhongKham) {
        return PhanHoiApi.ok(lichLamViecService.lichToanVien(tuNgay, denNgay, idChuyenKhoa, idBacSi, idPhongKham));
    }

    @Operation(summary = "Xếp 1 ca làm việc (sinh các lượt khám), có thể lặp lại theo thứ trong tuần tới 1 ngày; ngày bệnh"
            + " viện nghỉ, ngày trùng giờ với ca khác của bác sĩ / phòng khám hoặc vượt giờ làm trong tuần bị bỏ qua và"
            + " liệt kê trong boQua. Không lặp lại: ngày nghỉ -> 409 NGAY_BENH_VIEN_NGHI")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<KetQuaTaoCaResponse> tao(@Valid @RequestBody TaoCaRequest request) {
        return PhanHoiApi.ok(quanLyCaService.tao(NguoiDungHienTai.layIdTaiKhoan(), request), "thanhCong.xepCa");
    }

    @Operation(summary = "Xem trước sao chép ca: chạy đúng như khi sao chép rồi hoàn tác, trả các ca sẽ tạo và các ca bị bỏ"
            + " qua kèm lý do (ngày đã qua / quá xa / bệnh viện nghỉ, bác sĩ / phòng ngừng hoạt động, trùng giờ, vượt giờ"
            + " làm trong tuần)")
    @PostMapping("/sao-chep/xem-truoc")
    public PhanHoiApi<KetQuaSaoChepResponse> xemTruocSaoChep(@Valid @RequestBody SaoChepCaRequest request) {
        return PhanHoiApi.ok(quanLyCaService.saoChep(NguoiDungHienTai.layIdTaiKhoan(), request, false));
    }

    @Operation(summary = "Sao chép các ca còn hoạt động của khoảng nguồn (tối đa 42 ngày, lọc chuyên khoa / bác sĩ) sang"
            + " ngày đích cùng thứ, lặp lại 1-12 lần theo bước đủ tuần; bản sao vi phạm bị bỏ qua kèm lý do, mỗi bác sĩ"
            + " được báo 1 lần")
    @PostMapping("/sao-chep")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<KetQuaSaoChepResponse> saoChep(@Valid @RequestBody SaoChepCaRequest request) {
        return PhanHoiApi.ok(quanLyCaService.saoChep(NguoiDungHienTai.layIdTaiKhoan(), request, true),
                "thanhCong.saoChepCa");
    }

    // Không khai báo produces: lỗi (khoảng ngày sai...) vẫn trả JSON như các API khác
    @Operation(summary = "Tải tệp Excel các ca còn hoạt động trong khoảng ngày (tối đa 42 ngày) theo bộ lọc như lịch toàn"
            + " viện. Cột A-G cùng bố cục với tệp nhập, nên sửa rồi nhập lại được; cột sau G chỉ để xem")
    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> xuatExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idChuyenKhoa,
            @RequestParam(required = false) Long idBacSi,
            @RequestParam(required = false) Long idPhongKham) {
        byte[] tep = excelLichLamViecService.xuat(tuNgay, denNgay, idChuyenKhoa, idBacSi, idPhongKham);
        return TepTaiVe.taiVe(tep, "lich-lam-viec_" + tuNgay + "_" + denNgay + ".xlsx", TepTaiVe.XLSX);
    }

    @Operation(summary = "Tải tệp Excel mẫu để nhập lịch: trang ca (chỉ có tiêu đề), hướng dẫn, danh sách bác sĩ đang công"
            + " tác và phòng đang hoạt động (cột mã bác sĩ / mã phòng có danh sách chọn)")
    @GetMapping("/nhap-excel/mau")
    public ResponseEntity<byte[]> mauNhapExcel() {
        return TepTaiVe.taiVe(excelLichLamViecService.mau(), "mau-nhap-lich-lam-viec.xlsx", TepTaiVe.XLSX);
    }

    @Operation(summary = "Xem trước nhập lịch từ tệp .xlsx (phần tệp tên tep, tối đa 2MB, 1000 dòng): các dòng hợp lệ và"
            + " lỗi theo dòng / cột (ô sai dạng, trùng giờ trong tệp, không có bác sĩ / phòng, ngày đã qua / quá xa / bệnh"
            + " viện nghỉ, trùng giờ, vượt giờ làm trong tuần). Không lưu gì")
    @PostMapping(path = "/nhap-excel/xem-truoc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhanHoiApi<KetQuaNhapExcelResponse> xemTruocNhapExcel(@RequestPart("tep") MultipartFile tep) {
        return PhanHoiApi.ok(excelLichLamViecService.nhap(NguoiDungHienTai.layIdTaiKhoan(), tep, false));
    }

    @Operation(summary = "Nhập lịch từ tệp .xlsx, tất cả hoặc không: tệp còn lỗi -> 400 DU_LIEU_KHONG_HOP_LE, không lưu ca"
            + " nào. Mỗi bác sĩ được báo 1 lần")
    @PostMapping(path = "/nhap-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<KetQuaNhapExcelResponse> nhapExcel(@RequestPart("tep") MultipartFile tep) {
        return PhanHoiApi.ok(excelLichLamViecService.nhap(NguoiDungHienTai.layIdTaiKhoan(), tep, true),
                "thanhCong.nhapCa");
    }

    @Operation(summary = "Tải bản in PDF lịch làm việc của 1 bác sĩ trong khoảng ngày (tối đa 42 ngày, chỉ ca còn hoạt"
            + " động), chữ theo ngôn ngữ của request; cùng tệp bác sĩ tự tải được")
    @GetMapping("/pdf/bac-si/{idBacSi}")
    public ResponseEntity<byte[]> pdfBacSi(@PathVariable Long idBacSi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        TepPdf tep = pdfLichLamViecService.cuaBacSi(idBacSi, tuNgay, denNgay);
        return TepTaiVe.taiVe(tep.noiDung(), tep.tenTep(), MediaType.APPLICATION_PDF);
    }

    @Operation(summary = "Tải bản in PDF lưới tuần toàn viện (A4 ngang): mỗi tuần thứ Hai - Chủ nhật chạm khoảng ngày"
            + " (tối đa 42 ngày) là 1 trang, mỗi dòng 1 bác sĩ có ca, cột cuối là giờ làm trong tuần; lọc theo chuyên khoa")
    @GetMapping("/pdf/toan-vien")
    public ResponseEntity<byte[]> pdfToanVien(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idChuyenKhoa) {
        TepPdf tep = pdfLichLamViecService.toanVien(tuNgay, denNgay, idChuyenKhoa);
        return TepTaiVe.taiVe(tep.noiDung(), tep.tenTep(), MediaType.APPLICATION_PDF);
    }

    @Operation(summary = "Gửi lịch làm việc cho bác sĩ: mỗi bác sĩ có ca còn hoạt động từ hôm nay trong khoảng ngày (tối"
            + " đa 42 ngày; lọc chuyên khoa / bác sĩ) nhận 1 email liệt kê các ca kèm link trang Lịch làm việc và 1 thông"
            + " báo. Không có ca nào -> 400. Gửi lại thì gửi thêm lần nữa")
    @PostMapping("/gui-lich")
    public PhanHoiApi<KetQuaGuiLichResponse> guiLich(@Valid @RequestBody GuiLichRequest request) {
        return PhanHoiApi.ok(guiLichLamViecService.gui(NguoiDungHienTai.layIdTaiKhoan(), request), "thanhCong.guiLich");
    }

    @Operation(summary = "Sửa phòng khám, giờ, sức chứa của 1 ca chưa bắt đầu. Phòng đổi được bất cứ lúc nào; giờ / sức chứa"
            + " chỉ đổi được khi mọi lượt đã có người đặt còn nguyên (không thì 409 CA_CON_LICH_HEN)")
    @PutMapping("/{id}")
    public PhanHoiApi<CaLamViecResponse> sua(@PathVariable Long id, @Valid @RequestBody SuaCaRequest request) {
        return PhanHoiApi.ok(quanLyCaService.sua(NguoiDungHienTai.layIdTaiKhoan(), id, request),
                "thanhCong.suaCa");
    }

    @Operation(summary = "Hủy 1 ca chưa bắt đầu (bắt buộc có lý do). Lịch hẹn còn hiệu lực của ca được giữ và đánh dấu cần"
            + " đổi lịch, bệnh nhân và bác sĩ được báo")
    @PostMapping("/{id}/huy")
    public PhanHoiApi<KetQuaHuyCaResponse> huy(@PathVariable Long id, @Valid @RequestBody HuyCaRequest request) {
        return PhanHoiApi.ok(quanLyCaService.huy(NguoiDungHienTai.layIdTaiKhoan(), id, request.lyDo()),
                "thanhCong.huyCa");
    }

    @Operation(summary = "Lịch hẹn còn hiệu lực đang chờ bệnh nhân đổi lịch vì ca khám bị hủy, giờ khám cũ sớm nhất trước")
    @GetMapping("/can-doi-lich")
    public PhanHoiApi<TrangDuLieu<LichHenTrongCaResponse>> lichHenCanDoi(
            @RequestParam(defaultValue = "0") @Min(0) int trang,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int kichThuoc) {
        return PhanHoiApi.ok(quanLyCaService.lichHenCanDoi(trang, kichThuoc));
    }

    @Operation(summary = "Giờ làm của từng bác sĩ theo tuần thứ Hai - Chủ nhật (SCHED-05): số ca, số phút đã xếp, số phút"
            + " còn lại so với giới hạn; khoảng ngày (tối đa 42 ngày) được nới ra đủ các tuần; lọc theo chuyên khoa, bác sĩ")
    @GetMapping("/gio-lam-theo-tuan")
    public PhanHoiApi<GioLamTheoTuanResponse> gioLamTheoTuan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long idChuyenKhoa,
            @RequestParam(required = false) Long idBacSi) {
        return PhanHoiApi.ok(lichLamViecService.gioLamTheoTuan(tuNgay, denNgay, idChuyenKhoa, idBacSi));
    }

    @Operation(summary = "Lịch sử xếp / sửa / hủy của 1 ca, cũ nhất trước: ai làm, lúc nào, từ đâu, lý do, ca trước và"
            + " sau thay đổi")
    @GetMapping("/{id}/lich-su")
    public PhanHoiApi<List<LichSuCaResponse>> lichSu(@PathVariable Long id) {
        return PhanHoiApi.ok(lichLamViecService.lichSuCa(id));
    }

    @Operation(summary = "Lịch hẹn của 1 ca theo giờ khám (mọi trạng thái), kèm thông tin bệnh nhân")
    @GetMapping("/{id}/lich-hen")
    public PhanHoiApi<List<LichHenTrongCaResponse>> lichHenCuaCa(@PathVariable Long id) {
        return PhanHoiApi.ok(lichLamViecService.lichHenCuaCa(id));
    }

}
