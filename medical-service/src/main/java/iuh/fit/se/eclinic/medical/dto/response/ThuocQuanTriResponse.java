package iuh.fit.se.eclinic.medical.dto.response;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;

/**
 * 1 thuốc trong danh mục của quản trị viên. Dạng bào chế, đơn vị, mô tả là chữ đã lưu của cả 2 ngôn ngữ ({@code ...En} =
 * tiếng Anh, null khi chưa nhập).
 *
 * @param hoatChat    null ở thuốc bác sĩ đã thêm trước đây, chưa bổ sung (cả {@code hamLuong}); chưa có thì chưa xác minh
 *                    được
 * @param daXacMinh   false = bác sĩ tự thêm khi kê đơn (trước khi chỉ kê từ danh mục), quản trị viên chưa xác minh; không
 *                    kê mới được
 * @param trangThai   NGUNG_DUNG: không được gợi ý, không kê mới được; đơn thuốc cũ vẫn hiển thị
 * @param tenBacSiTao bác sĩ đã thêm thuốc khi kê đơn; null nếu do quản trị viên thêm hoặc có sẵn
 * @param soLanKe     số dòng đơn thuốc đang dùng thuốc này; lớn hơn 0 thì tên, hoạt chất, hàm lượng, dạng bào chế, đường
 *                    dùng không đổi được nữa
 */
public record ThuocQuanTriResponse(
        Long id,
        String tenThuoc,
        String hoatChat,
        String hamLuong,
        String dangBaoChe,
        String dangBaoCheEn,
        DuongDungThuoc duongDung,
        String donVi,
        String donViEn,
        String moTa,
        String moTaEn,
        boolean daXacMinh,
        TrangThaiThuoc trangThai,
        String tenBacSiTao,
        long soLanKe) {
}
