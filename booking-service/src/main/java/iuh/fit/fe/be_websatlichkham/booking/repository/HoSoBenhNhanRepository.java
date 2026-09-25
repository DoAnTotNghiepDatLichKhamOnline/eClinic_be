package iuh.fit.fe.be_websatlichkham.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.booking.HoSoBenhNhan;

public interface HoSoBenhNhanRepository extends JpaRepository<HoSoBenhNhan, Long> {

    Optional<HoSoBenhNhan> findByTaiKhoanId(Long taiKhoanId);

    /** Tra hồ sơ theo CCCD khi đặt lịch (BOOK-03): có rồi thì dùng lại, chưa có thì tạo mới. */
    Optional<HoSoBenhNhan> findByCccd(String cccd);

}
