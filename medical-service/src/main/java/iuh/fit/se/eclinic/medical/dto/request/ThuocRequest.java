package iuh.fit.se.eclinic.medical.dto.request;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu thêm / sửa 1 thuốc trong danh mục (quản trị viên). Thuốc phân biệt theo tên + hàm lượng.
 *
 * @param hoatChat     hoạt chất / tên chung quốc tế (Thông tư 26/2025/TT-BYT); thuốc phối hợp nối bằng " + "
 * @param hamLuong     vd 500mg, 250mg/5ml
 * @param dangBaoChe   viên nén, siro...
 * @param dangBaoCheEn dạng bào chế bằng tiếng Anh (tablet, syrup...); bỏ trống thì người xem tiếng Anh thấy bản tiếng Việt
 * @param duongDung    bỏ trống = chưa nhập
 * @param donVi        viên, ml, gói...
 * @param donViEn      đơn vị bằng tiếng Anh (tablet, ml, sachet...); bỏ trống thì người xem tiếng Anh thấy bản tiếng Việt
 * @param moTaEn       mô tả bằng tiếng Anh; tên thuốc, hoạt chất, hàm lượng không dịch
 */
public record ThuocRequest(
        @NotBlank(message = "{kiemTra.tenThuoc.batBuoc}")
        @Size(max = 255, message = "{kiemTra.tenThuoc.toiDa}")
        String tenThuoc,

        @NotBlank(message = "{kiemTra.hoatChat.batBuoc}")
        @Size(max = 255, message = "{kiemTra.hoatChat.toiDa}")
        String hoatChat,

        @NotBlank(message = "{kiemTra.hamLuong.batBuoc}")
        @Size(max = 100, message = "{kiemTra.hamLuong.toiDa}")
        String hamLuong,

        @Size(max = 50, message = "{kiemTra.dangBaoChe.toiDa}")
        String dangBaoChe,

        @Size(max = 50, message = "{kiemTra.dangBaoCheEn.toiDa}")
        String dangBaoCheEn,

        DuongDungThuoc duongDung,

        @Size(max = 30, message = "{kiemTra.donVi.toiDa}")
        String donVi,

        @Size(max = 30, message = "{kiemTra.donViEn.toiDa}")
        String donViEn,
        @Size(max = 2000, message = "{kiemTra.moTa.toiDa}")
        String moTa,

        @Size(max = 2000, message = "{kiemTra.moTaEn.toiDa}")
        String moTaEn) {
}
