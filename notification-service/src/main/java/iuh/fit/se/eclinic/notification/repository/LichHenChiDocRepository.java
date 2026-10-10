package iuh.fit.se.eclinic.notification.repository;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

/**
 * CHỈ ĐỌC lịch hẹn (bảng của booking-service) để biết lịch hẹn gắn với thông báo còn tồn tại. Kế thừa {@link Repository}
 * chứ không phải JpaRepository nên không có save / delete: notification-service không được ghi bảng này.
 */
public interface LichHenChiDocRepository extends Repository<LichHen, Long> {

    boolean existsById(Long id);

}
