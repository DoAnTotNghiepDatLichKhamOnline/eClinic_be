package iuh.fit.fe.be_websatlichkham.modules.schedule.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import iuh.fit.fe.be_websatlichkham.modules.schedule.enums.TimeSlotStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "time_slots", uniqueConstraints = @UniqueConstraint(name = "uk_time_slots_schedule_start",
        columnNames = { "schedule_id", "start_time" }))
public class TimeSlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WorkSchedule schedule;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private TimeSlotStatus status = TimeSlotStatus.AVAILABLE;

    /** Hết hạn giữ chỗ khi status = HELD (bệnh nhân đang điền form / thanh toán). */
    @Column(name = "held_until")
    private LocalDateTime heldUntil;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
