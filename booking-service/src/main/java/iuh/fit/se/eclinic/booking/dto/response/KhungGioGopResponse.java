package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/**
 * 1 khung giờ của chuyên khoa trong ngày, gộp các khung 1 giờ CÙNG GIỜ BẮT ĐẦU của mọi bác sĩ thuộc chuyên khoa
 * (dùng khi người đặt chọn "bác sĩ bất kỳ"). Khung của 2 ca bắt đầu lệch giờ nhau (08:00 và 08:30) là 2 dòng riêng.
 *
 * @param gioKetThuc  giờ kết thúc muộn nhất trong các khung được gộp
 * @param tongSoCho   tổng số lượt khám của các khung được gộp
 * @param soChoConLai tổng số lượt còn trống và còn kịp đặt
 * @param soBacSi     số bác sĩ có khung bắt đầu vào giờ này
 */
public record KhungGioGopResponse(
        LocalDateTime gioBatDau,
        LocalDateTime gioKetThuc,
        int tongSoCho,
        int soChoConLai,
        boolean hetCho,
        int soBacSi) {
}
