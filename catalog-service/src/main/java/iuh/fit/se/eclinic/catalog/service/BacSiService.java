package iuh.fit.se.eclinic.catalog.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.enums.BuoiKham;

public interface BacSiService {

    BacSi layTheoId(Long id);

    Optional<BacSi> timTheoTaiKhoanId(Long taiKhoanId);

    List<BacSi> timTheoChuyenKhoa(Long chuyenKhoaId);

    /** Danh sách bác sĩ công khai: đang công tác, tài khoản đã kích hoạt, theo tên. */
    TrangDuLieu<BacSiResponse> timKiem(Long idChuyenKhoa, String tuKhoa, LocalDate ngay, BuoiKham buoi, int trang,
            int kichThuoc);

    /** Ném KHONG_TIM_THAY nếu bác sĩ không tồn tại, ngừng công tác hoặc tài khoản không hoạt động. */
    BacSiChiTietResponse layChiTiet(Long id);

}
