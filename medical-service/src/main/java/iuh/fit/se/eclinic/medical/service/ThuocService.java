package iuh.fit.se.eclinic.medical.service;

import java.util.List;
import java.util.Set;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;

public interface ThuocService {

    /**
     * Lấy thuốc theo tên (không phân biệt hoa thường / khoảng trắng thừa), chưa có thì tạo mới
     * với {@code daXacMinh = false}. An toàn khi nhiều bác sĩ cùng thêm 1 tên thuốc.
     *
     * <p>
     * Ném THUOC_NGUNG_DUNG nếu thuốc đã ngừng dùng trong danh mục và không nằm trong {@code idThuocDaCoTrongDon}.
     *
     * @param idBacSiTao          bác sĩ đang kê đơn, có thể null
     * @param idThuocDaCoTrongDon các thuốc đơn thuốc đang sửa đã có từ trước: giữ lại được dù đã ngừng dùng, để việc
     *                            cho thuốc ngừng dùng không chặn bác sĩ sửa hồ sơ bệnh án cũ. Đơn thuốc mới: tập rỗng
     */
    Thuoc layHoacTao(String tenThuoc, String donVi, Long idBacSiTao, Set<Long> idThuocDaCoTrongDon);

    /** Gợi ý thuốc đang dùng theo từ khoá (tối đa 20); thuốc ngừng dùng không được gợi ý. */
    List<Thuoc> timKiem(String keyword);

}
