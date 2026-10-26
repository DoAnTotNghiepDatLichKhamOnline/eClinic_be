package iuh.fit.se.eclinic.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.booking.NhacLichHen;

public interface NhacLichHenRepository extends JpaRepository<NhacLichHen, Long> {

    boolean existsByIdLichHenAndSoPhutTruoc(Long idLichHen, int soPhutTruoc);

    /** Lịch hẹn đã được nhắc ở 1 mốc gần giờ khám hơn mốc này chưa. */
    boolean existsByIdLichHenAndSoPhutTruocLessThanAndDaGuiTrue(Long idLichHen, int soPhutTruoc);

}
