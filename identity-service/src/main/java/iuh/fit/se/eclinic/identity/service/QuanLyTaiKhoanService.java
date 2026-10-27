package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatTrangThaiTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.response.ChiTietTaiKhoanResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanQuanTriResponse;

/** Quản trị viên quản lý tài khoản: danh sách, chi tiết, vô hiệu hoá / kích hoạt lại, xoá. */
public interface QuanLyTaiKhoanService {

    /** Danh sách tài khoản, mới tạo đứng trước. */
    TrangDuLieu<TaiKhoanQuanTriResponse> timKiem(Long idNguoiThucHien, String tuKhoa, VaiTro vaiTro,
            TrangThaiTaiKhoan trangThai, int trang, int kichThuoc);

    /** Ném KHONG_TIM_THAY nếu không có tài khoản này. */
    ChiTietTaiKhoanResponse layChiTiet(Long idNguoiThucHien, Long id);

    /** Vô hiệu hoá (bắt buộc lý do) hoặc kích hoạt lại 1 tài khoản; cả 2 đều gửi email cho chủ tài khoản. */
    ChiTietTaiKhoanResponse capNhatTrangThai(Long idNguoiThucHien, Long id, CapNhatTrangThaiTaiKhoanRequest request);

    /** Xoá hẳn 1 tài khoản chưa được dữ liệu nào tham chiếu. */
    void xoa(Long idNguoiThucHien, Long id);

}
