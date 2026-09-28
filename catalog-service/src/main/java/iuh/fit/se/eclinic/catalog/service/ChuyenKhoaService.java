package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

/**
 * Quản lý chuyên khoa (ADM-01). Trả về DTO, controller không làm việc trực tiếp với entity.
 */
public interface ChuyenKhoaService {

    /** Tìm theo tên (không phân biệt hoa/thường, dấu); tuKhoa rỗng = lấy tất cả. Sắp xếp theo tên. */
    TrangDuLieu<ChuyenKhoaResponse> timKiem(String tuKhoa, int trang, int kichThuoc);

    ChuyenKhoaResponse layTheoId(Long id);

    /** Ném TEN_CHUYEN_KHOA_DA_TON_TAI nếu trùng tên. */
    ChuyenKhoaResponse tao(ChuyenKhoaRequest request);

    /** Ném TEN_CHUYEN_KHOA_DA_TON_TAI nếu trùng tên với chuyên khoa khác. */
    ChuyenKhoaResponse capNhat(Long id, ChuyenKhoaRequest request);

    /** Chỉ xoá được khi không còn phòng khám / bác sĩ nào thuộc chuyên khoa (CHUYEN_KHOA_DANG_DUOC_SU_DUNG). */
    void xoa(Long id);

}
