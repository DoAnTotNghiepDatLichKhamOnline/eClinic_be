package iuh.fit.se.eclinic.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

public interface BacSiRepository extends JpaRepository<BacSi, Long> {

    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

    List<BacSi> findByChuyenKhoaId(Long chuyenKhoaId);

    long countByChuyenKhoaId(Long chuyenKhoaId);

}
