package iuh.fit.se.eclinic.notification.repository;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/**
 * CHỈ ĐỌC tài khoản (bảng của identity-service) để biết người nhận thông báo còn tồn tại. Kế thừa {@link Repository} chứ
 * không phải JpaRepository nên không có save / delete: notification-service không được ghi bảng này.
 */
public interface TaiKhoanChiDocRepository extends Repository<TaiKhoan, Long> {

    boolean existsById(Long id);

}
