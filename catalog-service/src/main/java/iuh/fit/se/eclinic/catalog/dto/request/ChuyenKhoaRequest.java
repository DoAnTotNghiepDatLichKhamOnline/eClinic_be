package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu thêm / sửa chuyên khoa (ADM-01). 2 trường tiếng Anh có thể bỏ trống: khi đó người xem tiếng Anh thấy bản
 * tiếng Việt.
 *
 * @param trieuChung   triệu chứng, vấn đề thường gặp mà chuyên khoa tiếp nhận (vd "ho, sốt, đau họng"): chatbot dựa vào
 *                     đây để gợi ý chuyên khoa theo lời người dùng kể; không hiện ở API công khai
 * @param trieuChungEn bản tiếng Anh; bỏ trống thì chatbot dùng bản tiếng Việt
 */
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
