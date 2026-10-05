package iuh.fit.se.eclinic.catalog.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * Thứ tự hiển thị mới của ảnh giới thiệu: id của MỌI ảnh của bác sĩ, ảnh đứng trước hiện trước.
 */
public record SapXepAnhBacSiRequest(

        @NotNull(message = "Danh sách ảnh không được để trống")
        List<@NotNull(message = "Id ảnh không được để trống") Long> idAnh) {
}
