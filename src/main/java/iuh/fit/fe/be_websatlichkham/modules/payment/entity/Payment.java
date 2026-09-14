package iuh.fit.fe.be_websatlichkham.modules.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.AuditableEntity;
import iuh.fit.fe.be_websatlichkham.modules.appointment.entity.Appointment;
import iuh.fit.fe.be_websatlichkham.modules.payment.enums.PaymentMethod;
import iuh.fit.fe.be_websatlichkham.modules.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một lần thanh toán cho lịch hẹn. 1 appointment có thể có nhiều bản ghi (lịch sử FAILED, REFUNDED...)
 * nhưng tối đa 1 bản ghi PAID (DB: cột generated paid_appointment_id + UNIQUE).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    /** Copy từ doctors.consultation_fee tại thời điểm đặt. */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_code", length = 100, unique = true)
    private String transactionCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

}
