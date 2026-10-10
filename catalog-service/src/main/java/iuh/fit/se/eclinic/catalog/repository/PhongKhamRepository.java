package iuh.fit.se.eclinic.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import jakarta.persistence.LockModeType;

public interface PhongKhamRepository extends JpaRepository<PhongKham, Long> {

    List<PhongKham> findByTrangThaiOrderByTenPhongAsc(TrangThaiPhongKham trangThai);

    List<PhongKham> findByChuyenKhoaIdAndTrangThaiOrderByTenPhongAsc(Long chuyenKhoaId, TrangThaiPhongKham trangThai);

    long countByChuyenKhoaId(Long chuyenKhoaId);

    boolean existsByTenPhong(String tenPhong);

    boolean existsByTenPhongAndIdNot(String tenPhong, Long id);

    /**
     * Danh mục phòng khám của quản trị viên theo tên, kể cả phòng ngừng hoạt động, kèm chuyên khoa.
     *
     * @param mau mẫu LIKE (escape '!') so với tên phòng và tầng; null = không lọc
     */
    @Query(value = """
            select p from PhongKham p
              join fetch p.chuyenKhoa c
            where (:idChuyenKhoa is null or c.id = :idChuyenKhoa)
              and (:trangThai is null or p.trangThai = :trangThai)
              and (:mau is null or p.tenPhong like :mau escape '!' or p.tang like :mau escape '!')
            order by p.tenPhong asc, p.id asc
            """, countQuery = """
            select count(p) from PhongKham p
            where (:idChuyenKhoa is null or p.chuyenKhoa.id = :idChuyenKhoa)
              and (:trangThai is null or p.trangThai = :trangThai)
              and (:mau is null or p.tenPhong like :mau escape '!' or p.tang like :mau escape '!')
            """)
    Page<PhongKham> timChoQuanTri(String mau, Long idChuyenKhoa, TrangThaiPhongKham trangThai, Pageable pageable);

    /**
     * Khoá dòng phòng khám khi sửa / đổi trạng thái. booking-service khoá cùng dòng này khi tạo ca và khi đổi phòng của
     * ca, nên "ngừng hoạt động" và "xếp ca vào phòng" chạy lần lượt.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhongKham p join fetch p.chuyenKhoa where p.id = :id")
    Optional<PhongKham> findByIdForUpdate(Long id);

}
