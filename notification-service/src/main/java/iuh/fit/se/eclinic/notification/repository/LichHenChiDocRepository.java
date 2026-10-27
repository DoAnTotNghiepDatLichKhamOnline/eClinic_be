package iuh.fit.se.eclinic.notification.repository;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

/** CHỈ ĐỌC lịch hẹn (bảng của booking-service) để biết lịch hẹn gắn với thông báo còn tồn tại. */
public interface LichHenChiDocRepository extends Repository<LichHen, Long> {

    boolean existsById(Long id);

}
