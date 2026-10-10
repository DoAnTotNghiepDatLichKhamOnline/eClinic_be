package iuh.fit.se.eclinic.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

/**
 * CHỈ ĐỌC tài khoản (bảng của identity-service) để kiểm tra tài khoản còn hoạt động khi bệnh nhân đã đăng nhập đặt lịch,
 * xem lịch hẹn, sửa hồ sơ bệnh nhân. Kế thừa {@link Repository} chứ không phải JpaRepository nên không có save / delete:
 * booking-service không được ghi bảng này.
 */
public interface TaiKhoanChiDocRepository extends Repository<TaiKhoan, Long> {

    Optional<TaiKhoan> findById(Long id);

    /** Id các tài khoản của 1 vai trò đang ở 1 trạng thái (vd quản trị viên đang hoạt động, để gửi thông báo). */
    @Query("select t.id from TaiKhoan t where t.vaiTro = :vaiTro and t.trangThai = :trangThai order by t.id")
    List<Long> timIdTheoVaiTroVaTrangThai(VaiTro vaiTro, TrangThaiTaiKhoan trangThai);

    /** Cho Dashboard của quản trị viên (số tài khoản bệnh nhân đã kích hoạt). */
    long countByVaiTroAndTrangThai(VaiTro vaiTro, TrangThaiTaiKhoan trangThai);

}
