package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

/**
 * CHỈ ĐỌC hồ sơ bác sĩ (bảng của catalog-service) để biết bác sĩ nào đang đăng nhập khi xem lịch làm việc, lịch hẹn
 * của mình. Kế thừa {@link Repository} chứ không phải JpaRepository nên không có save / delete.
 */
public interface BacSiChiDocRepository extends Repository<BacSi, Long> {

    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

}
