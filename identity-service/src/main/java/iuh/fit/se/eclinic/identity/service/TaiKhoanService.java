package iuh.fit.se.eclinic.identity.service;

import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

public interface TaiKhoanService {

    TaiKhoan layTheoId(Long id);

    /** Tài khoản của người đang gọi API (id lấy từ JWT), đọc lại từ DB ở mỗi request. */
    TaiKhoan layDangHoatDong(Long id);

    /** Đặt ảnh đại diện của tài khoản đang hoạt động; {@code url} null là bỏ ảnh. */
    String capNhatAnhDaiDien(Long id, String url);

    Optional<TaiKhoan> timTheoEmail(String email);

    boolean tonTaiEmail(String email);

}
