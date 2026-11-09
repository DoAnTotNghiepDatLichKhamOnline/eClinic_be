package iuh.fit.se.eclinic.medical.dto.response;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import java.math.BigDecimal;

/**
 * 1 thuốc kê được (đang dùng, đã xác minh) trong danh mục, gợi ý khi bác sĩ gõ tên thuốc hoặc hoạt chất. Kèm BHYT và đơn
 * giá (VND / 1 donVi) để bác sĩ cân nhắc khi kê.
 */
public record ThuocGoiYResponse(Long id, String tenThuoc, String hoatChat, String hamLuong, String dangBaoChe,
        DuongDungThuoc duongDung, String donVi, boolean coBaoHiem, BigDecimal donGia) {
}
