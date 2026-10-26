package iuh.fit.se.eclinic.medical.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 1 dòng đơn thuốc. Thuốc chọn trong danh mục do quản trị viên quản lý (gợi ý: GET /api/medical/bac-si/toi/thuoc);
 * bác sĩ không tự thêm thuốc. Mỗi thuốc chỉ 1 dòng trong 1 đơn.
 *
 * @param idThuoc      thuốc đang dùng và đã xác minh; thuốc đã có sẵn trong đơn đang sửa thì giữ lại được dù đã ngừng
 *                     dùng / chưa xác minh
 * @param lieuDung     vd 500mg/lần
 * @param soNgayDung   bắt buộc; tối đa 30 ngày, hoặc 90 khi chẩn đoán chính có mã ICD-10 mạn tính (Thông tư 26/2025,
 *                     kiểm tra trong service)
 * @param soLuong      bắt buộc: tổng số lượng cấp, theo đơn vị của thuốc
 * @param ghiChuSuDung vd uống sau ăn
 */
public record DonThuocRequest(
        @NotNull(message = "{kiemTra.idThuoc.batBuoc}")
        Long idThuoc,

        @Size(max = 100, message = "{kiemTra.lieuDung.toiDa}")
        String lieuDung,

        @Min(value = 1, message = "{kiemTra.soLanMoiNgay.toiThieu}")
        Integer soLanMoiNgay,

        @NotNull(message = "{kiemTra.soNgayDung.batBuoc}")
        @Min(value = 1, message = "{kiemTra.soNgayDung.toiThieu}")
        @Max(value = 90, message = "{kiemTra.soNgayDung.toiDa}")
        Integer soNgayDung,

        @NotNull(message = "{kiemTra.soLuong.batBuoc}")
        @Min(value = 1, message = "{kiemTra.soLuong.khoang}")
        @Max(value = 9999, message = "{kiemTra.soLuong.khoang}")
        Integer soLuong,

        @Size(max = 255, message = "{kiemTra.ghiChuSuDung.toiDa}")
        String ghiChuSuDung) {
}
