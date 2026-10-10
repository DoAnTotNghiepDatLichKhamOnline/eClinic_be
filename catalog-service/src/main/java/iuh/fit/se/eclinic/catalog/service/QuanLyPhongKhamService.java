package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.PhongKhamRequest;
import iuh.fit.se.eclinic.catalog.dto.response.PhongKhamQuanTriResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;

/**
 * Danh mục phòng khám của quản trị viên. Phòng khám không bị xoá (ca làm việc và lịch hẹn tham chiếu tới): phòng không
 * dùng nữa thì cho ngừng hoạt động.
 */
public interface QuanLyPhongKhamService {

    /**
     * Mọi phòng khám theo tên, kể cả phòng đã ngừng hoạt động.
     *
     * @param tuKhoa so với tên phòng và tầng
     */
    TrangDuLieu<PhongKhamQuanTriResponse> danhSach(String tuKhoa, Long idChuyenKhoa, TrangThaiPhongKham trangThai,
            int trang, int kichThuoc);

    /** Ném KHONG_TIM_THAY (chuyên khoa), TEN_PHONG_DA_TON_TAI. */
    PhongKhamQuanTriResponse them(PhongKhamRequest request);

    /**
     * Tên phòng và tầng sửa được bất cứ lúc nào; chuyên khoa chỉ đổi được khi phòng không còn ca sắp tới (bác sĩ của các
     * ca đó thuộc chuyên khoa cũ).
     * <p>
     * Ném KHONG_TIM_THAY, TEN_PHONG_DA_TON_TAI, PHONG_CON_CA_LAM_VIEC.
     */
    PhongKhamQuanTriResponse sua(Long id, PhongKhamRequest request);

    /**
     * Phòng còn ca sắp tới thì từ chối (PHONG_CON_CA_LAM_VIEC, thông điệp có số ca): quản trị viên chuyển các ca đó sang
     * phòng khác trước. Phòng đã ngừng hoạt động thì không làm gì.
     */
    PhongKhamQuanTriResponse ngungHoatDong(Long id);

    PhongKhamQuanTriResponse hoatDongLai(Long id);
}
