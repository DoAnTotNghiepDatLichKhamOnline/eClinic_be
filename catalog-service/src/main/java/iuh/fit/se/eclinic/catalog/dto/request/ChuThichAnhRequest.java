package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.Size;

/** Bác sĩ sửa chú thích 1 ảnh làm việc của mình (không đổi loại ảnh, không đổi tệp ảnh). */
public record ChuThichAnhRequest(

        @Size(max = 200, message = "{kiemTra.chuThich.toiDa}")
        String chuThich,

        @Size(max = 200, message = "{kiemTra.chuThichEn.toiDa}")
        String chuThichEn) {
}
