package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import jakarta.persistence.LockModeType;

public interface HoSoBenhNhanRepository extends JpaRepository<HoSoBenhNhan, Long> {

    Optional<HoSoBenhNhan> findByTaiKhoanId(Long taiKhoanId);

    /** Tra hồ sơ theo CCCD khi đặt lịch (BOOK-03): có rồi thì dùng lại, chưa có thì tạo mới. */
    Optional<HoSoBenhNhan> findByCccd(String cccd);

    boolean existsByCccd(String cccd);

    /**
     * Như {@link #findByCccd} nhưng khoá dòng hồ sơ (SELECT ... FOR UPDATE) trong transaction đặt lịch, để 2 request
     * đặt lịch cho cùng 1 bệnh nhân chạy lần lượt: kiểm tra trùng giờ và đếm số lịch còn hiệu lực mới chính xác.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoSoBenhNhan h where h.cccd = :cccd")
    Optional<HoSoBenhNhan> findByCccdForUpdate(String cccd);

}
