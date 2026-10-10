package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import jakarta.persistence.LockModeType;

/**
 * CHỈ ĐỌC phòng khám (bảng của catalog-service) khi xếp / sửa ca làm việc. Kế thừa {@link Repository} chứ không phải
 * JpaRepository nên không có save / delete: booking-service không được ghi bảng này.
 */
public interface PhongKhamChiDocRepository extends Repository<PhongKham, Long> {

    @Query("select p from PhongKham p join fetch p.chuyenKhoa where p.id = :id")
    Optional<PhongKham> findById(Long id);

    /**
     * Khoá dòng phòng khám (SELECT ... FOR UPDATE; khoá không phải là ghi) khi xếp / sửa ca trong phòng đó: 2 quản trị
     * viên xếp 2 ca chồng giờ vào cùng phòng chạy lần lượt. Lấy sẵn chuyên khoa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhongKham p join fetch p.chuyenKhoa where p.id = :id")
    Optional<PhongKham> khoaTheoId(Long id);

}
