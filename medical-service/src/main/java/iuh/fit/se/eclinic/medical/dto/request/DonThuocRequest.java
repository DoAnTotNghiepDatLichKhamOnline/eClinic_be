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
        @NotBlank(message = "Tên thuốc không được để trống")
        @Size(max = 255, message = "Tên thuốc tối đa 255 ký tự")
        String tenThuoc,

        @Size(max = 30, message = "Đơn vị tối đa 30 ký tự")
        String donVi,

        @Size(max = 100, message = "Liều dùng tối đa 100 ký tự")
        String lieuDung,

        @Min(value = 1, message = "Số lần mỗi ngày phải từ 1 trở lên")
        Integer soLanMoiNgay,

        @Min(value = 1, message = "Số ngày dùng phải từ 1 trở lên")
        Integer soNgayDung,

        @Size(max = 255, message = "Ghi chú sử dụng tối đa 255 ký tự")
        String ghiChuSuDung) {
}
