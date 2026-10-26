package iuh.fit.se.eclinic.common.dto;

/**
 * Lỗi của một trường dữ liệu (vd. {truong: "email", thongDiep: "không được để trống"}).
 */
public record ChiTietLoi(String truong, String thongDiep) {
}
