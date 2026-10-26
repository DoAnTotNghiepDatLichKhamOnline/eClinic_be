package iuh.fit.se.eclinic.catalog.dto.response;

/** 1 phòng khám đang hoạt động, để chọn khi xếp ca làm việc. */
public record PhongKhamResponse(Long id, String tenPhong, String tang, Long idChuyenKhoa, String tenChuyenKhoa) {
}
