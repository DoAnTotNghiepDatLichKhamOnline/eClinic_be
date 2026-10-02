package iuh.fit.se.eclinic.identity.service;

import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

public interface TaiKhoanService {

    TaiKhoan layTheoId(Long id);

    /**
     * Tài khoản của người đang gọi API (id lấy từ JWT), đọc lại từ DB ở mỗi request. Mọi API "của tôi" (/api/users/me...)
     * phải lấy tài khoản qua đây: access token còn hạn nhưng tài khoản đã bị xoá thì báo CHUA_DANG_NHAP (401),
     * đã bị vô hiệu hoá thì báo TAI_KHOAN_BI_VO_HIEU_HOA (403).
     */
    TaiKhoan layDangHoatDong(Long id);

    /**
     * Đặt ảnh đại diện của tài khoản đang hoạt động (kiểm tra như {@link #layDangHoatDong}); {@code url} null là bỏ ảnh.
     *
     * @return URL ảnh trước đó, null nếu chưa có
     */
    String capNhatAnhDaiDien(Long id, String url);

    Optional<TaiKhoan> timTheoEmail(String email);

    boolean tonTaiEmail(String email);

}
