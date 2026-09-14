package iuh.fit.fe.be_websatlichkham.modules.medical.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.fe.be_websatlichkham.modules.medical.entity.Medication;
import jakarta.persistence.LockModeType;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    Optional<Medication> findByNormalizedName(String normalizedName);

    /**
     * Đọc có khoá (SELECT ... FOR SHARE): luôn thấy bản ghi mới nhất đã commit,
     * kể cả khi transaction hiện tại đã có snapshot cũ (REPEATABLE READ).
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select m from Medication m where m.normalizedName = :normalizedName")
    Optional<Medication> findByNormalizedNameForShare(String normalizedName);

    /** Gợi ý tên thuốc khi bác sĩ gõ. */
    List<Medication> findTop20ByNormalizedNameContainingOrderByNameAsc(String keyword);

    /**
     * Thêm thuốc nếu chưa có; trùng normalized_name thì bỏ qua (không ném lỗi, không làm hỏng transaction).
     */
    @Modifying
    @Query(value = """
            INSERT INTO medications (name, normalized_name, unit, is_verified, created_by_doctor_id)
            VALUES (:name, :normalizedName, :unit, FALSE, :createdByDoctorId)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfAbsent(String name, String normalizedName, String unit, Long createdByDoctorId);

}
