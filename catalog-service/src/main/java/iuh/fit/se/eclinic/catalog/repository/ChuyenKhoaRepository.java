package iuh.fit.se.eclinic.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;

/**
 * So sánh tên theo collation của cột (utf8mb4_unicode_ci): không phân biệt hoa/thường và dấu,
 * nên "noi tiet" trùng / tìm thấy "Nội tiết".
 */
public interface ChuyenKhoaRepository extends JpaRepository<ChuyenKhoa, Long> {

    boolean existsByTenChuyenKhoa(String tenChuyenKhoa);

    boolean existsByTenChuyenKhoaAndIdNot(String tenChuyenKhoa, Long id);

    Page<ChuyenKhoa> findByTenChuyenKhoaContaining(String tuKhoa, Pageable pageable);

}
