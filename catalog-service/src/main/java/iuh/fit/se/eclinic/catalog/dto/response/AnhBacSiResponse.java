package iuh.fit.se.eclinic.catalog.dto.response;

import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;

/**
 * 1 ảnh giới thiệu của bác sĩ. Không có mã ảnh trong kho.
 */
public record AnhBacSiResponse(
        Long id,
        LoaiAnhBacSi loai,
        String url,
        String chuThich) {
}
