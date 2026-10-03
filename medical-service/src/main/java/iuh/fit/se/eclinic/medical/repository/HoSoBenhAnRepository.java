package iuh.fit.se.eclinic.medical.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

public interface HoSoBenhAnRepository extends JpaRepository<HoSoBenhAn, Long> {

    Optional<HoSoBenhAn> findByLichHenId(Long lichHenId);

    List<HoSoBenhAn> findByHoSoBenhNhanIdOrderByNgayTaoDesc(Long hoSoBenhNhanId);

}
