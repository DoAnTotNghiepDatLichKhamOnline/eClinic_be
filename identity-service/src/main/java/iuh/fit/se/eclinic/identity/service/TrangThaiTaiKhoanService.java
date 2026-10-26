package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/** 2 bước đổi trạng thái tài khoản dùng chung cho API quản trị và API nội bộ. */
public interface TrangThaiTaiKhoanService {

    /** Chuyển sang VO_HIEU_HOA kèm lý do, thu hồi mọi phiên, huỷ mọi liên kết đã gửi, gửi email báo. */
    int voHieuHoa(TaiKhoan taiKhoan, String lyDo);

    /** Chuyển sang DA_KICH_HOAT, xoá lý do, gửi email báo chủ tài khoản. */
    void kichHoatLai(TaiKhoan taiKhoan);

    /** Huỷ mọi liên kết (xác thực email, đặt lại mật khẩu, đổi email) đang hiệu lực của tài khoản. */
    void huyMoiLienKet(Long idTaiKhoan);

}
