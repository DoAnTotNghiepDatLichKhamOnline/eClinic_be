package iuh.fit.se.eclinic.catalog.dto.request;

import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import jakarta.validation.constraints.Size;

/** Bác sĩ sửa 1 ảnh giới thiệu của mình (không đổi tệp ảnh). loai null = giữ loại đang có. */
public record ChuThichAnhRequest(

        @Size(max = 200, message = "{kiemTra.chuThich.toiDa}")
        String chuThich,

        @Size(max = 200, message = "{kiemTra.chuThichEn.toiDa}")
        String chuThichEn,

        LoaiAnhBacSi loai) {
}
