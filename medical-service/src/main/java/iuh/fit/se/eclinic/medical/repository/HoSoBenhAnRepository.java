package iuh.fit.se.eclinic.medical.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

public interface HoSoBenhAnRepository extends JpaRepository<HoSoBenhAn, Long> {

    Optional<HoSoBenhAn> findByLichHenId(Long lichHenId);

    /** Hồ sơ bệnh án của 1 lịch hẹn kèm các dòng đơn thuốc và thuốc. */
    @Query("""
            select distinct b from HoSoBenhAn b
            left join fetch b.chiTietDonThuoc c
            left join fetch c.thuoc
            where b.lichHen.id = :lichHenId
            """)
    Optional<HoSoBenhAn> timTheoLichHenKemDonThuoc(Long lichHenId);

    List<HoSoBenhAn> findByHoSoBenhNhanIdOrderByNgayTaoDesc(Long hoSoBenhNhanId);

}
