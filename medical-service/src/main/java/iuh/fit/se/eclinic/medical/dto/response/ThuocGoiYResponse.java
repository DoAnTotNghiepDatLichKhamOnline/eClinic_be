package iuh.fit.se.eclinic.medical.dto.response;

/**
 * 1 thuốc trong danh mục, gợi ý khi bác sĩ gõ tên thuốc.
 *
 * @param daXacMinh false = do bác sĩ tự thêm khi kê đơn, quản trị viên chưa duyệt
 */
public record ThuocGoiYResponse(Long id, String tenThuoc, String donVi, boolean daXacMinh) {
}
