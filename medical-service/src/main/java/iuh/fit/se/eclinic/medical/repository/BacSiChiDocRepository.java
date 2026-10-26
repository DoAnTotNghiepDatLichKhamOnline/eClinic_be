package iuh.fit.se.eclinic.medical.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

/** CHỈ ĐỌC hồ sơ bác sĩ (bảng của catalog-service) để biết bác sĩ nào đang đăng nhập khi khám bệnh. */
public interface BacSiChiDocRepository extends Repository<BacSi, Long> {

    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

}
