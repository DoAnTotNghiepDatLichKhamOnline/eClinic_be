package iuh.fit.se.eclinic.medical.dto.response;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;

/**
 * 1 thuốc kê được (đang dùng, đã xác minh) trong danh mục, gợi ý khi bác sĩ gõ tên thuốc hoặc hoạt chất. Dòng đơn thuốc
 * gửi {@code id} này.
 *
 * @param dangBaoChe theo ngôn ngữ người xem (vd viên nén / tablet); null nếu chưa nhập
 * @param duongDung  null nếu chưa nhập
 */
public record ThuocGoiYResponse(Long id, String tenThuoc, String hoatChat, String hamLuong, String dangBaoChe,
        DuongDungThuoc duongDung, String donVi) {
}
