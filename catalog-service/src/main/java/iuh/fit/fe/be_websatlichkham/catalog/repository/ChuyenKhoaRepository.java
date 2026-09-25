package iuh.fit.fe.be_websatlichkham.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.catalog.ChuyenKhoa;

public interface ChuyenKhoaRepository extends JpaRepository<ChuyenKhoa, Long> {

    Optional<ChuyenKhoa> findByTenChuyenKhoa(String tenChuyenKhoa);

}
