package iuh.fit.se.eclinic.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import iuh.fit.se.eclinic.common.exception.MaLoi;

/**
 * Khung JSON chung cho mọi API.
 * Thành công: {thanhCong: true, thongDiep, duLieu}. Lỗi: {thanhCong: false, maLoi, thongDiep, chiTiet}.
 * Trường null không được ghi ra JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PhanHoiApi<T>(boolean thanhCong, String maLoi, String thongDiep, T duLieu, List<ChiTietLoi> chiTiet) {

    public static <T> PhanHoiApi<T> ok(T duLieu) {
        return new PhanHoiApi<>(true, null, null, duLieu, null);
    }

    public static <T> PhanHoiApi<T> ok(T duLieu, String thongDiep) {
        return new PhanHoiApi<>(true, null, thongDiep, duLieu, null);
    }

    public static <T> PhanHoiApi<T> loi(MaLoi maLoi, String thongDiep) {
        return new PhanHoiApi<>(false, maLoi.name(), thongDiep, null, null);
    }

    public static <T> PhanHoiApi<T> loi(MaLoi maLoi, String thongDiep, List<ChiTietLoi> chiTiet) {
        return new PhanHoiApi<>(false, maLoi.name(), thongDiep, null, chiTiet);
    }

}
