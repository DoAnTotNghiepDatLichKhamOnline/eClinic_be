package iuh.fit.fe.be_websatlichkham.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.catalog.PhongKham;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiPhongKham;

public interface PhongKhamRepository extends JpaRepository<PhongKham, Long> {

    List<PhongKham> findByTrangThaiOrderByTenPhongAsc(TrangThaiPhongKham trangThai);

    List<PhongKham> findByChuyenKhoaIdAndTrangThaiOrderByTenPhongAsc(Long chuyenKhoaId, TrangThaiPhongKham trangThai);

}
