package iuh.fit.se.eclinic.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;

public interface PhongKhamRepository extends JpaRepository<PhongKham, Long> {

    List<PhongKham> findByTrangThaiOrderByTenPhongAsc(TrangThaiPhongKham trangThai);

    List<PhongKham> findByChuyenKhoaIdAndTrangThaiOrderByTenPhongAsc(Long chuyenKhoaId, TrangThaiPhongKham trangThai);

    long countByChuyenKhoaId(Long chuyenKhoaId);

}
