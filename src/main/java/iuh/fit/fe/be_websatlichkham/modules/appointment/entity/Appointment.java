package iuh.fit.fe.be_websatlichkham.modules.appointment.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.AuditableEntity;
import iuh.fit.fe.be_websatlichkham.modules.appointment.enums.AppointmentStatus;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Location;
import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.TimeSlot;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Doctor;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Patient;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lịch hẹn khám.
 * <p>
 * Chống trùng lịch ở mức DB: cột generated {@code active_slot_id} (không map trong entity) + UNIQUE,
 * nên 2 lịch hẹn có {@link AppointmentStatus#occupiesSlot()} trên cùng 1 slot sẽ ném
 * {@code DataIntegrityViolationException}.
 * <p>
 * Đổi lịch: lịch cũ -> CANCELLED_BY_RESCHEDULE, lịch mới có {@code rescheduledFrom} trỏ về lịch cũ.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimeSlot slot;

    /** Denormalized từ slot -> schedule -> room -> location, phải khớp với room của slot. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private AppointmentStatus status = AppointmentStatus.PENDING_CONFIRMATION;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescheduled_from_id")
    private Appointment rescheduledFrom;

    /** Người thao tác đặt: chính bệnh nhân, hoặc lễ tân/admin đặt hộ. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
