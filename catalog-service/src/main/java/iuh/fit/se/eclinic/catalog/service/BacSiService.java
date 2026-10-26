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

    /**
     * Danh sách bác sĩ công khai (DOC-03): chỉ bác sĩ đang công tác có tài khoản đã kích hoạt, sắp xếp theo tên.
     *
     * @param idChuyenKhoa null = mọi chuyên khoa
     * @param tuKhoa       tìm trong họ tên (không phân biệt hoa/thường, dấu); rỗng = không lọc
     * @param ngay         chỉ bác sĩ còn lượt khám đặt được trong ngày này; ngoài khoảng đặt lịch được (quá khứ, quá số
     *                     ngày đặt trước tối đa) = trang rỗng. null = mọi ngày còn đặt được
     * @param buoi         chỉ bác sĩ còn lượt khám đặt được bắt đầu trong buổi này (theo giờ bắt đầu của lượt). null cùng
     *                     {@code ngay} null = không lọc theo chỗ trống
     */
    TrangDuLieu<BacSiResponse> timKiem(Long idChuyenKhoa, String tuKhoa, LocalDate ngay, BuoiKham buoi, int trang,
            int kichThuoc);

    /** Ném KHONG_TIM_THAY nếu bác sĩ không tồn tại, ngừng công tác hoặc tài khoản không hoạt động. */
    BacSiChiTietResponse layChiTiet(Long id);

}
