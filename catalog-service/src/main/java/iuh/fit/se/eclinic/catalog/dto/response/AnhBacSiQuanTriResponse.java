package iuh.fit.se.eclinic.catalog.dto.response;

import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;

/**
 * 1 ảnh giới thiệu của bác sĩ cho quản trị viên: chú thích đã lưu của cả 2 ngôn ngữ. {@link AnhBacSiResponse} là bản công
 * khai, chỉ có chú thích của ngôn ngữ được yêu cầu.
 */
public record AnhBacSiQuanTriResponse(
        Long id,
        LoaiAnhBacSi loai,
        String url,
        String chuThich,
        String chuThichEn) {
}
