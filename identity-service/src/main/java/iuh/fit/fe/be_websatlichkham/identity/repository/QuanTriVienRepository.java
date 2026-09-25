package iuh.fit.fe.be_websatlichkham.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.identity.QuanTriVien;

public interface QuanTriVienRepository extends JpaRepository<QuanTriVien, Long> {

    Optional<QuanTriVien> findByTaiKhoanId(Long taiKhoanId);

}
