package iuh.fit.se.eclinic.medical.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/** CHỈ ĐỌC tài khoản (bảng của identity-service) để kiểm tra tài khoản bác sĩ còn hoạt động. */
public interface TaiKhoanChiDocRepository extends Repository<TaiKhoan, Long> {

    Optional<TaiKhoan> findById(Long id);

}
