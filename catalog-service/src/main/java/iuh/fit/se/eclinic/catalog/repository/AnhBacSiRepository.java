package iuh.fit.se.eclinic.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.catalog.AnhBacSi;

public interface AnhBacSiRepository extends JpaRepository<AnhBacSi, Long> {

    /** Ảnh của bác sĩ theo thứ tự hiển thị. */
    List<AnhBacSi> findByBacSiIdOrderByThuTuAscIdAsc(Long bacSiId);

    /** Ảnh của đúng bác sĩ này; ảnh của bác sĩ khác coi như không có. */
    Optional<AnhBacSi> findByIdAndBacSiId(Long id, Long bacSiId);

    long countByBacSiId(Long bacSiId);

    @Query("select coalesce(max(a.thuTu), 0) from AnhBacSi a where a.bacSi.id = :bacSiId")
    int timThuTuLonNhat(Long bacSiId);

}
