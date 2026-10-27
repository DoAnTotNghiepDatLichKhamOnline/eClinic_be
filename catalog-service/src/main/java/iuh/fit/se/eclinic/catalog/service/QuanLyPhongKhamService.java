package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.PhongKhamRequest;
import iuh.fit.se.eclinic.catalog.dto.response.PhongKhamQuanTriResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;

/** Danh mục phòng khám của quản trị viên. */
public interface QuanLyPhongKhamService {

    /** Mọi phòng khám theo tên, kể cả phòng đã ngừng hoạt động. */
    TrangDuLieu<PhongKhamQuanTriResponse> danhSach(String tuKhoa, Long idChuyenKhoa, TrangThaiPhongKham trangThai,
            int trang, int kichThuoc);

    /** Ném KHONG_TIM_THAY (chuyên khoa), TEN_PHONG_DA_TON_TAI. */
    PhongKhamQuanTriResponse them(PhongKhamRequest request);

    /** Tên phòng và tầng sửa được bất cứ lúc nào; chuyên khoa chỉ đổi khi phòng không còn ca sắp tới. */
    PhongKhamQuanTriResponse sua(Long id, PhongKhamRequest request);

    /** Phòng còn ca sắp tới thì từ chối, quản trị viên phải chuyển các ca đó sang phòng khác trước. */
    PhongKhamQuanTriResponse ngungHoatDong(Long id);

    PhongKhamQuanTriResponse hoatDongLai(Long id);
}
