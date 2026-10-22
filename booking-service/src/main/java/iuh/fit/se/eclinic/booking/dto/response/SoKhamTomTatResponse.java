package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Phần đầu của sổ khám bệnh, ai có link cũng xem được (EXAM-03): đủ để nhận ra lượt khám, không có kết quả khám.
 *
 * @param hoTenChe     họ tên người khám chỉ giữ tên, phần còn lại viết tắt (vd N. V. An)
 * @param namSinh      năm sinh người khám; null nếu không có ngày sinh
 * @param gioKham      giờ bắt đầu lượt khám được xếp
 * @param tenChuyenKhoa theo ngôn ngữ người xem
 */
public record SoKhamTomTatResponse(String hoTenChe, Integer namSinh, LocalDate ngayKham, LocalTime gioKham,
        BacSiTomTatResponse bacSi, String tenChuyenKhoa, PhongKhamTomTatResponse phongKham) {
}
