package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/**
 * 2 bước đổi trạng thái tài khoản dùng chung cho API của quản trị viên ({@link QuanLyTaiKhoanService}) và API nội bộ
 * ({@link TaiKhoanNoiBoService}). Không kiểm tra quyền hay điều kiện nghiệp vụ: nơi gọi tự kiểm tra, và phải gọi trong
 * transaction của mình với {@code taiKhoan} đang được quản lý.
 */
public interface TrangThaiTaiKhoanService {

    /**
     * Chuyển sang VO_HIEU_HOA kèm lý do, thu hồi mọi phiên đăng nhập, huỷ mọi liên kết đã gửi, gửi email báo chủ tài
     * khoản. Sau khi gọi, {@code taiKhoan} không còn được quản lý (UPDATE thu hồi phiên clear persistence context).
     *
     * @return số phiên bị thu hồi
     */
    int voHieuHoa(TaiKhoan taiKhoan, String lyDo);

    /** Chuyển sang DA_KICH_HOAT, xoá lý do, gửi email báo chủ tài khoản. Không khôi phục phiên nào. */
    void kichHoatLai(TaiKhoan taiKhoan);

    /** Huỷ mọi liên kết (xác thực email, đặt lại mật khẩu, đổi email) đang hiệu lực của tài khoản. */
    void huyMoiLienKet(Long idTaiKhoan);

}
