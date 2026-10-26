package iuh.fit.se.eclinic.medical.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;

public interface ThuocService {

    /**
     * Thuốc của các dòng đơn thuốc, theo id. Dòng mới chỉ kê được thuốc đang dùng và đã xác minh.
     *
     * <p>
     * Theo thứ tự trong danh sách, thuốc đầu tiên không hợp lệ làm hỏng cả đơn: không có trong danh mục → KHONG_TIM_THAY;
     * ngừng dùng → THUOC_NGUNG_DUNG; chưa xác minh → THUOC_CHUA_XAC_MINH.
     *
     * @param idCacThuoc          id thuốc của các dòng, theo thứ tự trong request (không trùng nhau)
     * @param idThuocDaCoTrongDon các thuốc đơn thuốc đang sửa đã có từ trước: giữ lại được dù đã ngừng dùng hoặc chưa xác
     *                            minh, để việc quản trị viên đổi danh mục không chặn bác sĩ sửa hồ sơ bệnh án cũ. Đơn
     *                            thuốc mới: tập rỗng
     */
    Map<Long, Thuoc> layDeKe(List<Long> idCacThuoc, Set<Long> idThuocDaCoTrongDon);

    /** Gợi ý thuốc kê được (đang dùng, đã xác minh) theo một phần tên hoặc hoạt chất, tối đa 20. */
    List<Thuoc> timKiem(String keyword);

}
