package iuh.fit.se.eclinic.identity.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/**
 * CHỈ ĐỌC hồ sơ bệnh nhân (bảng của booking-service) để hiển thị trong hồ sơ cá nhân và trong quản lý tài khoản. Kế thừa
 * {@link Repository} chứ không phải JpaRepository nên không có save / delete: identity-service không được ghi bảng này.
 */
public interface HoSoBenhNhanChiDocRepository extends Repository<HoSoBenhNhan, Long> {

    Optional<HoSoBenhNhan> findByTaiKhoanId(Long taiKhoanId);

    boolean existsByTaiKhoanId(Long taiKhoanId);

    /** Hồ sơ của nhiều tài khoản trong 1 truy vấn (danh sách tài khoản của quản trị viên). */
    List<HoSoBenhNhan> findByTaiKhoanIdInAndTrangThaiLienKet(Collection<Long> taiKhoanIds,
            TrangThaiLienKet trangThaiLienKet);

}
