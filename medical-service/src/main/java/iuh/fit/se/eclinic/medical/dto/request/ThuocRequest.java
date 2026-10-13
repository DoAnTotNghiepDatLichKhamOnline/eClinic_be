package iuh.fit.se.eclinic.medical.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu thêm / sửa 1 thuốc trong danh mục (quản trị viên).
 *
 * @param donVi   viên, ml, gói...
 * @param donViEn đơn vị bằng tiếng Anh (tablet, ml, sachet...); bỏ trống thì người xem tiếng Anh thấy bản tiếng Việt
 * @param moTaEn  mô tả bằng tiếng Anh; tên thuốc không dịch
 */
public record ThuocRequest(
        @NotBlank(message = "{kiemTra.tenThuoc.batBuoc}")
        @Size(max = 255, message = "{kiemTra.tenThuoc.toiDa}")
        String tenThuoc,
        @Size(max = 30, message = "{kiemTra.donVi.toiDa}")
        String donVi,

        @Size(max = 30, message = "{kiemTra.donViEn.toiDa}")
        String donViEn,
        @Size(max = 2000, message = "{kiemTra.moTa.toiDa}")
        String moTa,

        @Size(max = 2000, message = "{kiemTra.moTaEn.toiDa}")
        String moTaEn) {
}
