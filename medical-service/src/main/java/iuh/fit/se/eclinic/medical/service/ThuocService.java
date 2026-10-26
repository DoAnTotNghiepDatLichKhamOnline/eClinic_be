package iuh.fit.se.eclinic.medical.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;

public interface ThuocService {

    /** Thuốc của các dòng đơn thuốc, theo id. */
    Map<Long, Thuoc> layDeKe(List<Long> idCacThuoc, Set<Long> idThuocDaCoTrongDon);

    /** Gợi ý thuốc kê được (đang dùng, đã xác minh) theo một phần tên hoặc hoạt chất, tối đa 20. */
    List<Thuoc> timKiem(String keyword);

}
