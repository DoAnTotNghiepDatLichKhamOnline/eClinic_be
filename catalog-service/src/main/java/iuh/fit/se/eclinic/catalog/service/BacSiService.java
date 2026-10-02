package iuh.fit.se.eclinic.catalog.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

public interface BacSiService {

    BacSi layTheoId(Long id);

    Optional<BacSi> timTheoTaiKhoanId(Long taiKhoanId);

    List<BacSi> timTheoChuyenKhoa(Long chuyenKhoaId);

    /**
     * Danh sách bác sĩ công khai (DOC-03): chỉ bác sĩ đang công tác có tài khoản đã kích hoạt, sắp xếp theo tên.
     *
     * @param idChuyenKhoa null = mọi chuyên khoa
     * @param tuKhoa       tìm trong họ tên (không phân biệt hoa/thường, dấu); rỗng = không lọc
     */
    TrangDuLieu<BacSiResponse> timKiem(Long idChuyenKhoa, String tuKhoa, int trang, int kichThuoc);

    /** Ném KHONG_TIM_THAY nếu bác sĩ không tồn tại, ngừng công tác hoặc tài khoản không hoạt động. */
    BacSiChiTietResponse layChiTiet(Long id);

}
