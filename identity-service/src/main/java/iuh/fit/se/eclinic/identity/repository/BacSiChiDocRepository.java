package iuh.fit.se.eclinic.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

/** CHỈ ĐỌC hồ sơ bác sĩ (bảng của catalog-service) cho hồ sơ cá nhân và quản lý tài khoản. */
public interface BacSiChiDocRepository extends Repository<BacSi, Long> {

    @EntityGraph(attributePaths = "chuyenKhoa")
    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

    boolean existsByTaiKhoanId(Long taiKhoanId);

}
