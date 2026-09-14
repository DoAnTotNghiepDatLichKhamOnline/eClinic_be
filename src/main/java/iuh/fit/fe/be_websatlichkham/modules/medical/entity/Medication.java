package iuh.fit.fe.be_websatlichkham.modules.medical.entity;

import iuh.fit.fe.be_websatlichkham.common.entity.CreatableEntity;
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
 * Danh mục thuốc. Bác sĩ tự thêm khi kê toa thông qua MedicationService.getOrCreate
 * (unique theo normalized_name). {@code verified} để dành cho tính năng admin duyệt thuốc sau này.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medications")
public class Medication extends CreatableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true)
    private String normalizedName;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_doctor_id")
    private Doctor createdByDoctor;

}
