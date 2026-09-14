package iuh.fit.fe.be_websatlichkham.modules.schedule.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import iuh.fit.fe.be_websatlichkham.common.entity.AuditableEntity;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Room;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Doctor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ca làm việc của bác sĩ tại 1 phòng trong 1 ngày. Được chia thành các {@link TimeSlot}.
 * Không được chồng giờ theo bác sĩ hoặc theo phòng (kiểm ở WorkScheduleService.assertNoOverlap).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "work_schedules")
public class WorkSchedule extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes = 30;

}
