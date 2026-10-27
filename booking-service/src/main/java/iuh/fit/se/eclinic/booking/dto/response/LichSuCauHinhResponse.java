package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse.QuanTriThucHien;

/** 1 trường cấu hình đổi giá trị trong 1 lần lưu. 1 lần lưu đổi 3 trường thì có 3 dòng cùng {@code thoiDiem}. */
public record LichSuCauHinhResponse(
        Long id,
        String truong,
        String giaTriCu,
        String giaTriMoi,
        LocalDateTime thoiDiem,
        QuanTriThucHien quanTri) {
}
