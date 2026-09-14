package iuh.fit.fe.be_websatlichkham.modules.medical.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.AuditableEntity;
import iuh.fit.fe.be_websatlichkham.modules.appointment.entity.Appointment;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Doctor;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Patient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hồ sơ bệnh án của 1 lần khám (1-1 với appointment), kèm toa thuốc.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medical_records")
public class MedicalRecord extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Gợi ý ngày tái khám. KHÔNG tự tạo appointment, chỉ dùng để nhắc qua notifications. */
    @Column(name = "next_visit_suggested_date")
    private LocalDate nextVisitSuggestedDate;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> prescriptionItems = new ArrayList<>();

    public void addPrescriptionItem(PrescriptionItem item) {
        prescriptionItems.add(item);
        item.setMedicalRecord(this);
    }

    public void removePrescriptionItem(PrescriptionItem item) {
        prescriptionItems.remove(item);
        item.setMedicalRecord(null);
    }

}
