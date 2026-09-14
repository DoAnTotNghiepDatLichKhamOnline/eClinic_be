package iuh.fit.fe.be_websatlichkham.modules.medical.entity;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
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
 * 1 dòng thuốc trong toa. Thêm qua {@link MedicalRecord#addPrescriptionItem(PrescriptionItem)}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    /** vd: 500mg/lần */
    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "frequency_per_day")
    private Integer frequencyPerDay;

    @Column(name = "duration_days")
    private Integer durationDays;

    /** vd: uống sau ăn */
    @Column(name = "usage_note")
    private String usageNote;

}
