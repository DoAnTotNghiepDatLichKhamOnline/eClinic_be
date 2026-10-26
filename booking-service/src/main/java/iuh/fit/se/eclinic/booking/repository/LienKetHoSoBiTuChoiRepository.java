package iuh.fit.se.eclinic.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.booking.LienKetHoSoBiTuChoi;

public interface LienKetHoSoBiTuChoiRepository extends JpaRepository<LienKetHoSoBiTuChoi, Long> {

    /** Quản trị viên đã từ chối gắn hồ sơ này vào tài khoản này? Có thì không đưa lại vào hàng chờ xác minh. */
    boolean existsByTaiKhoanIdAndHoSoBenhNhanId(Long taiKhoanId, Long hoSoBenhNhanId);

}
