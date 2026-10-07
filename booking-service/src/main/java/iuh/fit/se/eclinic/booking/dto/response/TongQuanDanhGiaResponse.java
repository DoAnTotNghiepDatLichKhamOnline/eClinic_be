package iuh.fit.se.eclinic.booking.dto.response;

import java.util.Map;

/**
 * Điểm đánh giá của 1 bác sĩ.
 *
 * @param diemTrungBinh trung bình cộng, 1 chữ số thập phân; null khi chưa có đánh giá nào
 * @param phanBo        số đánh giá theo từng mức sao, luôn đủ 5 khoá 1..5
 */
public record TongQuanDanhGiaResponse(Double diemTrungBinh, long soDanhGia, Map<Integer, Long> phanBo) {
}
