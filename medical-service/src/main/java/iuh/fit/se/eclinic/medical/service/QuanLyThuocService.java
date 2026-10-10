package iuh.fit.se.eclinic.medical.service;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import iuh.fit.se.eclinic.medical.dto.request.ThuocRequest;
import iuh.fit.se.eclinic.medical.dto.response.ThuocQuanTriResponse;

/**
 * Danh mục thuốc của quản trị viên. Thuốc không bị xoá và không gộp được (dòng đơn thuốc tham chiếu tới thuốc): thuốc
 * không dùng nữa thì cho ngừng dùng.
 */
public interface QuanLyThuocService {

    /**
     * Thuốc chưa xác minh đứng trước, rồi theo tên.
     *
     * @param tuKhoa    so với tên thuốc, không phân biệt hoa thường
     * @param daXacMinh null = cả hai
     * @param trangThai null = mọi trạng thái
     */
    TrangDuLieu<ThuocQuanTriResponse> danhSach(String tuKhoa, Boolean daXacMinh, TrangThaiThuoc trangThai, int trang,
            int kichThuoc);

    /** Thuốc do quản trị viên thêm là thuốc đã xác minh. Ném TEN_THUOC_DA_TON_TAI. */
    ThuocQuanTriResponse them(ThuocRequest request);

    /**
     * Đơn vị và mô tả sửa được bất cứ lúc nào. Thuốc đã có trong đơn thuốc thì chỉ sửa được cách viết hoa / khoảng trắng
     * của tên: dòng đơn thuốc chỉ lưu id thuốc, đổi sang tên khác là sửa luôn các đơn thuốc cũ.
     * <p>
     * Ném KHONG_TIM_THAY, THUOC_DA_DUOC_KE, TEN_THUOC_DA_TON_TAI.
     */
    ThuocQuanTriResponse sua(Long id, ThuocRequest request);

    ThuocQuanTriResponse xacMinh(Long id);

    ThuocQuanTriResponse ngungDung(Long id);

    ThuocQuanTriResponse dungLai(Long id);
}
