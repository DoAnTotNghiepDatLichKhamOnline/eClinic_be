package iuh.fit.se.eclinic.medical.dto.response;

import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;

/**
 * 1 thuốc trong danh mục của quản trị viên.
 *
 * @param daXacMinh   false = bác sĩ tự thêm khi kê đơn, quản trị viên chưa xác minh
 * @param trangThai   NGUNG_DUNG: không được gợi ý, không kê mới được; đơn thuốc cũ vẫn hiển thị
 * @param tenBacSiTao bác sĩ đã thêm thuốc khi kê đơn; null nếu do quản trị viên thêm hoặc có sẵn
 * @param soLanKe     số dòng đơn thuốc đang dùng thuốc này; lớn hơn 0 thì không đổi sang tên khác được
 */
public record ThuocQuanTriResponse(
        Long id,
        String tenThuoc,
        String donVi,
        String moTa,
        boolean daXacMinh,
        TrangThaiThuoc trangThai,
        String tenBacSiTao,
        long soLanKe) {
}
