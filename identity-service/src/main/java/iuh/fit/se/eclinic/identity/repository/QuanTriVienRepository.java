package iuh.fit.se.eclinic.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;

public interface QuanTriVienRepository extends JpaRepository<QuanTriVien, Long> {

    Optional<QuanTriVien> findByTaiKhoanId(Long taiKhoanId);

}
