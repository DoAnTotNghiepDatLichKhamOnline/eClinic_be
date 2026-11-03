package iuh.fit.se.eclinic.catalog.service;

import java.util.List;

import iuh.fit.se.eclinic.catalog.dto.request.PhongKhamRequest;
import iuh.fit.se.eclinic.catalog.dto.response.PhongKhamQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.PhongKhamResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;

/** Phòng khám đang hoạt động và danh mục phòng khám của quản trị viên. */
public interface PhongKhamService {

    PhongKham layTheoId(Long id);

    /** Các phòng khám đang hoạt động. */
    List<PhongKham> layDangHoatDong();

    /** Các phòng khám đang hoạt động của 1 chuyên khoa. */
    List<PhongKham> layDangHoatDongTheoChuyenKhoa(Long chuyenKhoaId);

    /** Phòng khám đang hoạt động kèm tên chuyên khoa; {@code idChuyenKhoa} null là mọi chuyên khoa. */
    List<PhongKhamResponse> danhSachDangHoatDong(Long idChuyenKhoa);

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
