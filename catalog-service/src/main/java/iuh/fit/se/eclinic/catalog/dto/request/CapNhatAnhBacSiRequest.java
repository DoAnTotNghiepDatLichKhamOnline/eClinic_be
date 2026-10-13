package iuh.fit.se.eclinic.catalog.dto.request;

import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Sửa loại và chú thích của 1 ảnh giới thiệu (không đổi tệp ảnh: muốn đổi thì xoá rồi tải ảnh mới).
 */
public record CapNhatAnhBacSiRequest(

        @NotNull(message = "{kiemTra.loaiAnh.batBuoc}")
        LoaiAnhBacSi loai,

        @Size(max = 200, message = "{kiemTra.chuThich.toiDa}")
        String chuThich,

        @Size(max = 200, message = "{kiemTra.chuThichEn.toiDa}")
        String chuThichEn) {
}
