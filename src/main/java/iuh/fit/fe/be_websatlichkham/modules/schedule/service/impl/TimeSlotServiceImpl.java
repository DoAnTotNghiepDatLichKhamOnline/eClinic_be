package iuh.fit.fe.be_websatlichkham.modules.schedule.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.TimeSlot;
import iuh.fit.fe.be_websatlichkham.modules.schedule.enums.TimeSlotStatus;
import iuh.fit.fe.be_websatlichkham.modules.schedule.repository.TimeSlotRepository;
import iuh.fit.fe.be_websatlichkham.modules.schedule.service.TimeSlotService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    @Override
    public TimeSlot getById(Long id) {
        return timeSlotRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TimeSlot", id));
    }

    @Override
    public List<TimeSlot> findBySchedule(Long scheduleId) {
        return timeSlotRepository.findByScheduleIdOrderByStartTimeAsc(scheduleId);
    }

    @Override
    public List<TimeSlot> findAvailableByDoctor(Long doctorId, LocalDateTime from, LocalDateTime to) {
        return timeSlotRepository.findByDoctorAndStatusInRange(doctorId, TimeSlotStatus.AVAILABLE, from, to);
    }

}
