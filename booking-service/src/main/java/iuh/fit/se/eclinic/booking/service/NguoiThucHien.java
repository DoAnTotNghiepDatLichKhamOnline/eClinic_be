package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.LoaiNguoiThucHien;

/** Ai làm 1 thao tác trên lịch hẹn, để ghi nhật ký lịch hẹn ({@link LichSuLichHenService}). */
public record NguoiThucHien(LoaiNguoiThucHien loai, TaiKhoan taiKhoan) {

    public static final NguoiThucHien KHACH = new NguoiThucHien(LoaiNguoiThucHien.KHACH, null);

    /** Tài khoản bệnh nhân đã đăng nhập. */
    public static NguoiThucHien taiKhoan(TaiKhoan taiKhoan) {
        return new NguoiThucHien(LoaiNguoiThucHien.TAI_KHOAN, taiKhoan);
    }

    public static NguoiThucHien bacSi(BacSi bacSi) {
        return new NguoiThucHien(LoaiNguoiThucHien.BAC_SI, bacSi.getTaiKhoan());
    }

    public static NguoiThucHien quanTri(QuanTriVien quanTri) {
        return new NguoiThucHien(LoaiNguoiThucHien.QUAN_TRI_VIEN, quanTri.getTaiKhoan());
    }

    /** Người đặt 1 lịch hẹn mới: tài khoản đã đặt, hoặc khách. */
    public static NguoiThucHien nguoiDat(TaiKhoan taiKhoanDat) {
        return taiKhoanDat == null ? KHACH : taiKhoan(taiKhoanDat);
    }

}
