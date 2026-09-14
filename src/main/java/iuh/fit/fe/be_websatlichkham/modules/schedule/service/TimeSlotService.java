package iuh.fit.fe.be_websatlichkham.modules.schedule.service;

import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.TimeSlot;

public interface TimeSlotService {

    TimeSlot getById(Long id);

    List<TimeSlot> findBySchedule(Long scheduleId);

    /** Các slot còn trống của bác sĩ, bắt đầu trong khoảng [from, to). */
    List<TimeSlot> findAvailableByDoctor(Long doctorId, LocalDateTime from, LocalDateTime to);

}
