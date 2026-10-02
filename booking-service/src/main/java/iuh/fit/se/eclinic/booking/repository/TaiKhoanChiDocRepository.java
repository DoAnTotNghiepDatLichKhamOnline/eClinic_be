package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/**
 * CHỈ ĐỌC tài khoản (bảng của identity-service) để kiểm tra tài khoản còn hoạt động khi bệnh nhân đã đăng nhập đặt lịch,
 * xem lịch hẹn, sửa hồ sơ bệnh nhân. Kế thừa {@link Repository} chứ không phải JpaRepository nên không có save / delete:
 * booking-service không được ghi bảng này.
 */
public interface TaiKhoanChiDocRepository extends Repository<TaiKhoan, Long> {

    Optional<TaiKhoan> findById(Long id);

}
