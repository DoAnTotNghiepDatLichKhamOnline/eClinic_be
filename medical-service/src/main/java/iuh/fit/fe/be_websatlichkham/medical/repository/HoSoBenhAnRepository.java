package iuh.fit.fe.be_websatlichkham.medical.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.medical.HoSoBenhAn;

public interface HoSoBenhAnRepository extends JpaRepository<HoSoBenhAn, Long> {

    Optional<HoSoBenhAn> findByLichHenId(Long lichHenId);

    List<HoSoBenhAn> findByHoSoBenhNhanIdOrderByNgayTaoDesc(Long hoSoBenhNhanId);

}
