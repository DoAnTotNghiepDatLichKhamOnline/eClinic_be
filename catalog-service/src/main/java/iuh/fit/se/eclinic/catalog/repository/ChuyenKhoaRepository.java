package iuh.fit.se.eclinic.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;

/**
 * So sánh tên theo collation của cột (utf8mb4_unicode_ci): không phân biệt hoa/thường và dấu,
 * nên "noi tiet" trùng / tìm thấy "Nội tiết".
 */
public interface ChuyenKhoaRepository extends JpaRepository<ChuyenKhoa, Long> {

    boolean existsByTenChuyenKhoa(String tenChuyenKhoa);

    boolean existsByTenChuyenKhoaAndIdNot(String tenChuyenKhoa, Long id);

    boolean existsByTenChuyenKhoaEn(String tenChuyenKhoaEn);

    boolean existsByTenChuyenKhoaEnAndIdNot(String tenChuyenKhoaEn, Long id);

    /**
     * Sắp xếp theo tên tiếng Việt. Từ khoá so với tên của cả 2 ngôn ngữ, dù request đang ở ngôn ngữ nào.
     *
     * @param mau mẫu LIKE (escape '!'); null = không lọc
     */
    @Query(value = """
            select c from ChuyenKhoa c
            where (:mau is null or c.tenChuyenKhoa like :mau escape '!' or c.tenChuyenKhoaEn like :mau escape '!')
            order by c.tenChuyenKhoa, c.id
            """, countQuery = """
            select count(c) from ChuyenKhoa c
            where (:mau is null or c.tenChuyenKhoa like :mau escape '!' or c.tenChuyenKhoaEn like :mau escape '!')
            """)
    Page<ChuyenKhoa> timXepTheoTen(String mau, Pageable pageable);

    /** Như {@link #timXepTheoTen} nhưng sắp xếp theo tên hiện cho người xem tiếng Anh (chưa có tên tiếng Anh: tên Việt). */
    @Query(value = """
            select c from ChuyenKhoa c
            where (:mau is null or c.tenChuyenKhoa like :mau escape '!' or c.tenChuyenKhoaEn like :mau escape '!')
            order by coalesce(c.tenChuyenKhoaEn, c.tenChuyenKhoa), c.id
            """, countQuery = """
            select count(c) from ChuyenKhoa c
            where (:mau is null or c.tenChuyenKhoa like :mau escape '!' or c.tenChuyenKhoaEn like :mau escape '!')
            """)
    Page<ChuyenKhoa> timXepTheoTenTiengAnh(String mau, Pageable pageable);

}
