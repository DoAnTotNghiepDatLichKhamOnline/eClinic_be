package iuh.fit.se.eclinic.booking.dto.response;

import java.util.Map;

/** Điểm đánh giá của 1 bác sĩ. */
public record TongQuanDanhGiaResponse(Double diemTrungBinh, long soDanhGia, Map<Integer, Long> phanBo) {
}
