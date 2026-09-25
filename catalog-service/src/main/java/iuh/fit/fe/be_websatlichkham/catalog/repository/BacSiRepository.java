package iuh.fit.fe.be_websatlichkham.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;

public interface BacSiRepository extends JpaRepository<BacSi, Long> {

    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

    List<BacSi> findByChuyenKhoaId(Long chuyenKhoaId);

}
