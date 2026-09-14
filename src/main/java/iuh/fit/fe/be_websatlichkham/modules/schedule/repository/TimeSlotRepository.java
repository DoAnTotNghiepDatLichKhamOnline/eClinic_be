package iuh.fit.fe.be_websatlichkham.modules.schedule.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.TimeSlot;
import iuh.fit.fe.be_websatlichkham.modules.schedule.enums.TimeSlotStatus;
import jakarta.persistence.LockModeType;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByScheduleIdOrderByStartTimeAsc(Long scheduleId);

    List<TimeSlot> findByScheduleIdAndStatusOrderByStartTimeAsc(Long scheduleId, TimeSlotStatus status);

    /** Các slot của bác sĩ có trạng thái cho trước, bắt đầu trong khoảng [from, to). */
    @Query("""
            select ts from TimeSlot ts
            where ts.schedule.doctor.id = :doctorId
              and ts.status = :status
              and ts.startTime >= :from
              and ts.startTime < :to
            order by ts.startTime
            """)
    List<TimeSlot> findByDoctorAndStatusInRange(Long doctorId, TimeSlotStatus status, LocalDateTime from,
            LocalDateTime to);

    /**
     * Khoá slot (SELECT ... FOR UPDATE) trong transaction đặt lịch, để 2 request đặt cùng slot chạy tuần tự.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ts from TimeSlot ts where ts.id = :id")
    Optional<TimeSlot> findByIdForUpdate(Long id);

}
