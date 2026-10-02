package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatTrangThaiTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.response.ChiTietTaiKhoanResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanQuanTriResponse;

/**
 * Quản trị viên quản lý tài khoản người dùng (UC-USER-01, ADM-03): xem danh sách, xem chi tiết, vô hiệu hoá / kích hoạt
 * lại, xoá.
 * <p>
 * {@code idNguoiThucHien} là tài khoản của quản trị viên đang gọi API (lấy từ JWT) và được đọc lại từ DB ở MỖI lời gọi:
 * tài khoản đó không còn thì ném CHUA_DANG_NHAP, đã bị vô hiệu hoá thì TAI_KHOAN_BI_VO_HIEU_HOA, không phải quản trị
 * viên thì KHONG_CO_QUYEN (access token của người vừa bị vô hiệu hoá còn hạn tới 30 phút).
 * <p>
 * Tài khoản quản trị viên không vô hiệu hoá và không xoá được qua đây (TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE): hệ thống không
 * bao giờ mất quản trị viên cuối cùng và không ai tự khoá chính mình.
 */
public interface QuanLyTaiKhoanService {

    /**
     * Danh sách tài khoản, mới tạo đứng trước.
     *
     * @param tuKhoa    tìm trong họ tên, email, số điện thoại (không phân biệt hoa thường và dấu; % và _ là ký tự
     *                  thường); null / rỗng là không lọc
     * @param vaiTro    null là mọi vai trò
     * @param trangThai null là mọi trạng thái
     */
    TrangDuLieu<TaiKhoanQuanTriResponse> timKiem(Long idNguoiThucHien, String tuKhoa, VaiTro vaiTro,
            TrangThaiTaiKhoan trangThai, int trang, int kichThuoc);

    /** Ném KHONG_TIM_THAY nếu không có tài khoản này. */
    ChiTietTaiKhoanResponse layChiTiet(Long idNguoiThucHien, Long id);

    /**
     * Vô hiệu hoá ({@code VO_HIEU_HOA}, bắt buộc có lý do) hoặc kích hoạt lại ({@code DA_KICH_HOAT}) 1 tài khoản; cả 2
     * đều gửi email báo cho chủ tài khoản. Vô hiệu hoá thì đăng xuất mọi thiết bị và huỷ mọi liên kết đang chờ (kích hoạt,
     * đặt lại mật khẩu, đổi email) của tài khoản.
     * <p>
     * Ném KHONG_TIM_THAY; TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE; DU_LIEU_KHONG_HOP_LE (yêu cầu chuyển sang CHO_XAC_NHAN, hoặc vô
     * hiệu hoá mà thiếu lý do); TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE (tài khoản chưa xác thực email: chỉ có thể xoá; hoặc
     * tài khoản đã ở sẵn trạng thái đó); BAC_SI_CON_LICH_HEN (bác sĩ còn lịch hẹn chờ xác nhận / đã xác nhận chưa diễn
     * ra, UC-DOCT-03).
     *
     * @return chi tiết tài khoản sau khi đổi
     */
    ChiTietTaiKhoanResponse capNhatTrangThai(Long idNguoiThucHien, Long id, CapNhatTrangThaiTaiKhoanRequest request);

    /**
     * Xoá hẳn 1 tài khoản chưa được dữ liệu nào tham chiếu (thực tế: tài khoản đăng ký chưa xác thực, bệnh nhân chưa có
     * hồ sơ / thông báo / phiên chat). Phiên đăng nhập, liên kết đang chờ và ảnh đại diện trong kho bị xoá theo.
     * Không gửi email.
     * <p>
     * Ném KHONG_TIM_THAY; TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE; TAI_KHOAN_DANG_DUOC_SU_DUNG (thông điệp nêu dữ liệu đang tham
     * chiếu; khi đó hãy vô hiệu hoá).
     */
    void xoa(Long idNguoiThucHien, Long id);

}
