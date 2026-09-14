package iuh.fit.fe.be_websatlichkham.modules.schedule.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.WorkSchedule;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    List<WorkSchedule> findByDoctorIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(Long doctorId,
            LocalDate from, LocalDate to);

    /**
     * Bác sĩ đã có ca khác chồng giờ trong cùng ngày? (2 khoảng [s1,e1) và [s2,e2) chồng nhau khi s1 < e2 và e1 > s2)
     *
     * @param excludeId id ca đang sửa (bỏ qua chính nó), truyền null khi tạo mới
     */
    @Query("""
            select count(ws) > 0 from WorkSchedule ws
            where ws.doctor.id = :doctorId
              and ws.workDate = :workDate
              and ws.startTime < :endTime
              and ws.endTime > :startTime
              and (:excludeId is null or ws.id <> :excludeId)
            """)
    boolean existsOverlapForDoctor(Long doctorId, LocalDate workDate, LocalTime startTime, LocalTime endTime,
            Long excludeId);

    /** Phòng đã có ca khác (của bác sĩ bất kỳ) chồng giờ trong cùng ngày? */
    @Query("""
            select count(ws) > 0 from WorkSchedule ws
            where ws.room.id = :roomId
              and ws.workDate = :workDate
              and ws.startTime < :endTime
              and ws.endTime > :startTime
              and (:excludeId is null or ws.id <> :excludeId)
            """)
    boolean existsOverlapForRoom(Long roomId, LocalDate workDate, LocalTime startTime, LocalTime endTime,
            Long excludeId);

}
