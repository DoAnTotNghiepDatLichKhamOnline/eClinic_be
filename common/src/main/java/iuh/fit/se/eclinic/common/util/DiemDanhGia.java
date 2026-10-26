package iuh.fit.se.eclinic.common.util;

/** Điểm đánh giá trung bình hiển thị với 1 chữ số thập phân (3.666 -> 3.7). */
public final class DiemDanhGia {

    private DiemDanhGia() {
    }

    /** @return null nếu chưa có đánh giá nào */
    public static Double lamTron(Double diemTrungBinh) {
        return diemTrungBinh == null ? null : Math.round(diemTrungBinh * 10) / 10.0;
    }
}
