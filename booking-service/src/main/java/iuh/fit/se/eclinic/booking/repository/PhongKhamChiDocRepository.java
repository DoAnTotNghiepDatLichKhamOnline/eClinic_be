package iuh.fit.se.eclinic.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import jakarta.persistence.LockModeType;

/** CHỈ ĐỌC phòng khám (bảng của catalog-service) khi xếp / sửa ca làm việc. */
public interface PhongKhamChiDocRepository extends Repository<PhongKham, Long> {

    @Query("select p from PhongKham p join fetch p.chuyenKhoa where p.id = :id")
    Optional<PhongKham> findById(Long id);

    /** Khoá dòng phòng khám (SELECT ... */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhongKham p join fetch p.chuyenKhoa where p.id = :id")
    Optional<PhongKham> khoaTheoId(Long id);

    /** Phòng đang hoạt động, lấy sẵn chuyên khoa: danh sách phòng của tệp mẫu nhập lịch. */
    @Query("""
            select p from PhongKham p join fetch p.chuyenKhoa
            where p.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham.HOAT_DONG
            order by p.id
            """)
    List<PhongKham> timDangHoatDong();

}
