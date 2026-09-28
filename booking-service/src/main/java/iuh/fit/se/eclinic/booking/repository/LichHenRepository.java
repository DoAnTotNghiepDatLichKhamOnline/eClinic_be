package iuh.fit.se.eclinic.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

public interface LichHenRepository extends JpaRepository<LichHen, Long> {

    List<LichHen> findByHoSoBenhNhanIdOrderByNgayTaoDesc(Long hoSoBenhNhanId);

    List<LichHen> findByBacSiIdAndTrangThaiOrderByNgayTaoDesc(Long bacSiId, TrangThaiLichHen trangThai);

    boolean existsByKhungGioIdAndTrangThaiIn(Long khungGioId, List<TrangThaiLichHen> trangThais);

}
