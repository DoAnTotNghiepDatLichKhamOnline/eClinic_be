package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

/** Quản lý chuyên khoa. */
public interface ChuyenKhoaService {

    /** Tìm theo tên ở cả 2 ngôn ngữ (không phân biệt hoa/thường, dấu); tuKhoa rỗng = lấy tất cả. */
    TrangDuLieu<ChuyenKhoaResponse> timKiem(String tuKhoa, int trang, int kichThuoc);

    /** Cho quản trị viên: như {@link #timKiem} nhưng trả chữ đã lưu của cả 2 ngôn ngữ, sắp xếp theo tên tiếng Việt. */
    TrangDuLieu<ChuyenKhoaQuanTriResponse> danhSachQuanTri(String tuKhoa, int trang, int kichThuoc);

    ChuyenKhoaResponse layTheoId(Long id);

    /** Ném TEN_CHUYEN_KHOA_DA_TON_TAI nếu trùng tên tiếng Việt, hoặc tên tiếng Anh trùng tên tiếng Anh đã có. */
    ChuyenKhoaQuanTriResponse tao(ChuyenKhoaRequest request);

    /** Ném TEN_CHUYEN_KHOA_DA_TON_TAI nếu trùng tên với chuyên khoa khác. */
    ChuyenKhoaQuanTriResponse capNhat(Long id, ChuyenKhoaRequest request);

    /** Chỉ xoá được khi không còn phòng khám / bác sĩ nào thuộc chuyên khoa (CHUYEN_KHOA_DANG_DUOC_SU_DUNG). */
    void xoa(Long id);

}
