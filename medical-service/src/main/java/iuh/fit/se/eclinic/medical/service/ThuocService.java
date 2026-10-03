package iuh.fit.se.eclinic.medical.service;

import java.util.List;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;

public interface ThuocService {

    /**
     * Lấy thuốc theo tên (không phân biệt hoa thường / khoảng trắng thừa), chưa có thì tạo mới
     * với {@code daXacMinh = false}. An toàn khi nhiều bác sĩ cùng thêm 1 tên thuốc.
     *
     * @param idBacSiTao bác sĩ đang kê đơn, có thể null
     */
    Thuoc layHoacTao(String tenThuoc, String donVi, Long idBacSiTao);

    /** Gợi ý thuốc theo từ khoá (tối đa 20). */
    List<Thuoc> timKiem(String keyword);

}
