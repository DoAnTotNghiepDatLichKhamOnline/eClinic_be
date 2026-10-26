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
     * Thuốc chưa xác minh đứng trước, rồi theo tên, hàm lượng.
     *
     * @param tuKhoa    so với tên thuốc hoặc hoạt chất, không phân biệt hoa thường
     * @param daXacMinh null = cả hai
     * @param trangThai null = mọi trạng thái
     */
    TrangDuLieu<ThuocQuanTriResponse> danhSach(String tuKhoa, Boolean daXacMinh, TrangThaiThuoc trangThai, int trang,
            int kichThuoc);

    /** Thuốc do quản trị viên thêm là thuốc đã xác minh. Ném TEN_THUOC_DA_TON_TAI (trùng tên + hàm lượng). */
    ThuocQuanTriResponse them(ThuocRequest request);

    /**
     * Đơn vị, mô tả, dạng bào chế tiếng Anh sửa được bất cứ lúc nào. Thuốc đã có trong đơn thuốc thì tên (trừ cách viết hoa
     * / khoảng trắng), hoạt chất, hàm lượng, dạng bào chế, đường dùng không đổi được, chỉ điền được trường còn trống: dòng
     * đơn thuốc chỉ lưu id thuốc, đổi những trường này là sửa luôn các đơn thuốc cũ.
     * <p>
     * Ném KHONG_TIM_THAY, THUOC_DA_DUOC_KE, TEN_THUOC_DA_TON_TAI.
     */
    ThuocQuanTriResponse sua(Long id, ThuocRequest request);

    /** Ném THUOC_THIEU_THONG_TIN nếu thuốc chưa có hoạt chất hoặc hàm lượng. */
    ThuocQuanTriResponse xacMinh(Long id);

    ThuocQuanTriResponse ngungDung(Long id);

    ThuocQuanTriResponse dungLai(Long id);
}
