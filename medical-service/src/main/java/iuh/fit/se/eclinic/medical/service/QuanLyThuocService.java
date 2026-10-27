package iuh.fit.se.eclinic.medical.service;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import iuh.fit.se.eclinic.medical.dto.request.ThuocRequest;
import iuh.fit.se.eclinic.medical.dto.response.ThuocQuanTriResponse;

/** Danh mục thuốc của quản trị viên. */
public interface QuanLyThuocService {

    /** Thuốc chưa xác minh đứng trước, rồi theo tên, hàm lượng. */
    TrangDuLieu<ThuocQuanTriResponse> danhSach(String tuKhoa, Boolean daXacMinh, TrangThaiThuoc trangThai, int trang,
            int kichThuoc);

    /** Thuốc do quản trị viên thêm là thuốc đã xác minh. Ném TEN_THUOC_DA_TON_TAI (trùng tên + hàm lượng). */
    ThuocQuanTriResponse them(ThuocRequest request);

    /** Đơn vị, mô tả, dạng bào chế tiếng Anh sửa được bất cứ lúc nào. */
    ThuocQuanTriResponse sua(Long id, ThuocRequest request);

    /** Ném THUOC_THIEU_THONG_TIN nếu thuốc chưa có hoạt chất hoặc hàm lượng. */
    ThuocQuanTriResponse xacMinh(Long id);

    ThuocQuanTriResponse ngungDung(Long id);

    ThuocQuanTriResponse dungLai(Long id);
}
