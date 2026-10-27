package iuh.fit.se.eclinic.medical.dto.request;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dữ liệu thêm / sửa 1 thuốc trong danh mục (quản trị viên). */
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
