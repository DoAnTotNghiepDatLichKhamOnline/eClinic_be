package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.persistence.LockModeType;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

/**
 * CHỈ ĐỌC hồ sơ bác sĩ (bảng của catalog-service) để biết bác sĩ nào đang đăng nhập khi xem lịch làm việc, lịch hẹn
 * của mình. Kế thừa {@link Repository} chứ không phải JpaRepository nên không có save / delete.
 */
public interface BacSiChiDocRepository extends Repository<BacSi, Long> {

    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

    /**
     * Khoá dòng bác sĩ (SELECT ... FOR UPDATE; khoá không phải là ghi) khi xếp / sửa ca của bác sĩ đó: 2 quản trị viên
     * xếp 2 ca chồng giờ cho cùng bác sĩ chạy lần lượt, người sau thấy ca của người trước. Lấy sẵn tài khoản, chuyên khoa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BacSi b join fetch b.taiKhoan join fetch b.chuyenKhoa where b.id = :id")
    Optional<BacSi> khoaTheoId(Long id);

    /** Số bác sĩ đang công tác có tài khoản đã kích hoạt: đúng tập bác sĩ của danh sách công khai (catalog-service). */
    @Query("""
            select count(b) from BacSi b
            where b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and b.taiKhoan.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
            """)
    long demDangCongTacCoTaiKhoanHoatDong();

}
