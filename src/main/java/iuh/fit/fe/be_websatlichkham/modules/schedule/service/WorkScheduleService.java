package iuh.fit.fe.be_websatlichkham.modules.schedule.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.WorkSchedule;

public interface WorkScheduleService {

    WorkSchedule getById(Long id);

    List<WorkSchedule> findByDoctor(Long doctorId, LocalDate from, LocalDate to);

    /**
     * Ném BusinessException(SCHEDULE_OVERLAP) nếu ca [startTime, endTime) trong ngày workDate
     * chồng giờ với ca khác của cùng bác sĩ hoặc cùng phòng.
     *
     * @param excludeScheduleId id ca đang sửa, null khi tạo mới
     */
    void assertNoOverlap(Long doctorId, Long roomId, LocalDate workDate, LocalTime startTime, LocalTime endTime,
            Long excludeScheduleId);

}
