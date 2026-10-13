package iuh.fit.se.eclinic.medical.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 1 dòng đơn thuốc. Thuốc ghi bằng tên: tên chưa có trong danh mục thì được thêm vào danh mục (chưa xác minh).
 *
 * @param donVi        đơn vị của thuốc (viên, ml, gói...), chỉ dùng khi tên thuốc này chưa có trong danh mục
 * @param lieuDung     vd 500mg/lần
 * @param ghiChuSuDung vd uống sau ăn
 */
public record DonThuocRequest(
        @NotBlank(message = "{kiemTra.tenThuoc.batBuoc}")
        @Size(max = 255, message = "{kiemTra.tenThuoc.toiDa}")
        String tenThuoc,

        @Size(max = 30, message = "{kiemTra.donVi.toiDa}")
        String donVi,

        @Size(max = 100, message = "{kiemTra.lieuDung.toiDa}")
        String lieuDung,

        @Min(value = 1, message = "{kiemTra.soLanMoiNgay.toiThieu}")
        Integer soLanMoiNgay,

        @Min(value = 1, message = "{kiemTra.soNgayDung.toiThieu}")
        Integer soNgayDung,

        @Size(max = 255, message = "{kiemTra.ghiChuSuDung.toiDa}")
        String ghiChuSuDung) {
}
