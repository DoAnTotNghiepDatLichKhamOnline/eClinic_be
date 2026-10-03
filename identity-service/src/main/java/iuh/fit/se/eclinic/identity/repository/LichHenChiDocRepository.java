package iuh.fit.se.eclinic.identity.repository;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * CHỈ ĐỌC lịch hẹn (bảng của booking-service) để kiểm tra trước khi vô hiệu hoá tài khoản bác sĩ, xoá tài khoản. Kế thừa
 * {@link Repository} chứ không phải JpaRepository nên không có save / delete: identity-service không được ghi bảng này.
 */
public interface LichHenChiDocRepository extends Repository<LichHen, Long> {

    /** Số lịch hẹn ở các trạng thái đã cho của bác sĩ (theo tài khoản) mà khung giờ kết thúc sau {@code bayGio}. */
    @Query("select count(l) from LichHen l where l.bacSi.taiKhoan.id = :taiKhoanId and l.trangThai in :trangThai"
            + " and l.khungGio.gioKetThuc > :bayGio")
    long countSapToiCuaBacSi(Long taiKhoanId, Collection<TrangThaiLichHen> trangThai, LocalDateTime bayGio);

    /** Tài khoản đã đặt lịch hẹn nào khi đăng nhập chưa (lich_hen.id_tai_khoan_dat, khoá ngoại RESTRICT). */
    boolean existsByTaiKhoanDatId(Long taiKhoanId);

}
