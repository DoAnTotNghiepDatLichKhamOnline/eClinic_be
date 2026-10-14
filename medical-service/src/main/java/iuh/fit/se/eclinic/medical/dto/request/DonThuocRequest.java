package iuh.fit.se.eclinic.medical.dto.request;

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
 * @param ghiChuSuDung vd uống sau ăn
 */
public record DonThuocRequest(
        @NotNull(message = "{kiemTra.idThuoc.batBuoc}")
        Long idThuoc,

        @Size(max = 100, message = "{kiemTra.lieuDung.toiDa}")
        String lieuDung,

        @Min(value = 1, message = "{kiemTra.soLanMoiNgay.toiThieu}")
        Integer soLanMoiNgay,

        @Min(value = 1, message = "{kiemTra.soNgayDung.toiThieu}")
        Integer soNgayDung,

        @Size(max = 255, message = "{kiemTra.ghiChuSuDung.toiDa}")
        String ghiChuSuDung) {
}
