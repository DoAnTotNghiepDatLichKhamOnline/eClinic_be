package iuh.fit.se.eclinic.medical.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import jakarta.persistence.LockModeType;

/** Lịch hẹn (bảng của booking-service) cho việc khám bệnh. */
public interface LichHenKhamRepository extends Repository<LichHen, Long> {

    Optional<LichHen> findById(Long id);

    /** Khoá dòng lịch hẹn: 2 lần ghi nhận cùng lúc chạy lần lượt, lần sau thấy lịch hẹn đã hoàn thành. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LichHen l where l.id = :id")
    Optional<LichHen> findByIdForUpdate(Long id);

    /** Bác sĩ có lịch hẹn với hồ sơ bệnh nhân này từ {@code gioKham} trở đi không. */
    @Query("""
            select count(l) > 0 from LichHen l
            where l.bacSi.id = :idBacSi and l.hoSoBenhNhan.id = :idHoSoBenhNhan and l.khungGio.gioBatDau >= :gioKham
            """)
    boolean bacSiXemDuocLuotKham(Long idBacSi, Long idHoSoBenhNhan, LocalDateTime gioKham);

}
