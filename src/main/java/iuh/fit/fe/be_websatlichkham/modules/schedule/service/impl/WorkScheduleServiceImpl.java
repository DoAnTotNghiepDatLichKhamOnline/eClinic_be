package iuh.fit.fe.be_websatlichkham.modules.schedule.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.BusinessException;
import iuh.fit.fe.be_websatlichkham.common.exception.ErrorCode;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.WorkSchedule;
import iuh.fit.fe.be_websatlichkham.modules.schedule.repository.WorkScheduleRepository;
import iuh.fit.fe.be_websatlichkham.modules.schedule.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;

    @Override
    public WorkSchedule getById(Long id) {
        return workScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkSchedule", id));
    }

    @Override
    public List<WorkSchedule> findByDoctor(Long doctorId, LocalDate from, LocalDate to) {
        return workScheduleRepository.findByDoctorIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(doctorId, from,
                to);
    }

    @Override
    public void assertNoOverlap(Long doctorId, Long roomId, LocalDate workDate, LocalTime startTime,
            LocalTime endTime, Long excludeScheduleId) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (workScheduleRepository.existsOverlapForDoctor(doctorId, workDate, startTime, endTime,
                excludeScheduleId)) {
            throw new BusinessException(ErrorCode.SCHEDULE_OVERLAP, "Bác sĩ đã có ca làm việc trùng giờ");
        }
        if (workScheduleRepository.existsOverlapForRoom(roomId, workDate, startTime, endTime, excludeScheduleId)) {
            throw new BusinessException(ErrorCode.SCHEDULE_OVERLAP, "Phòng đã được xếp ca trùng giờ");
        }
    }

}
