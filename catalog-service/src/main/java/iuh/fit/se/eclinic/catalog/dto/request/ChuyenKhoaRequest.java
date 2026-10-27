package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Thêm, sửa chuyên khoa; bỏ trống tiếng Anh thì người xem tiếng Anh thấy bản tiếng Việt. */
public record ChuyenKhoaRequest(

        @NotBlank(message = "{kiemTra.tenChuyenKhoa.batBuoc}")
        @Size(max = 150, message = "{kiemTra.tenChuyenKhoa.toiDa}")
        String tenChuyenKhoa,

        @Size(max = 150, message = "{kiemTra.tenChuyenKhoaEn.toiDa}")
        String tenChuyenKhoaEn,

        @Size(max = 2000, message = "{kiemTra.moTa.toiDa}")
        String moTa,

        @Size(max = 2000, message = "{kiemTra.moTaEn.toiDa}")
        String moTaEn,

        @Size(max = 2000, message = "{kiemTra.trieuChung.toiDa}")
        String trieuChung,

        @Size(max = 2000, message = "{kiemTra.trieuChungEn.toiDa}")
        String trieuChungEn) {
}
