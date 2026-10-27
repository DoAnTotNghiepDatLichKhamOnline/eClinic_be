package iuh.fit.se.eclinic.notification.repository;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/** CHỈ ĐỌC tài khoản (bảng của identity-service) để biết người nhận thông báo còn tồn tại. */
public interface TaiKhoanChiDocRepository extends Repository<TaiKhoan, Long> {

    boolean existsById(Long id);

}
