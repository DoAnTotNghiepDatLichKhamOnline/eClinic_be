package iuh.fit.se.eclinic.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.booking.NguoiThanDaLuu;

public interface NguoiThanDaLuuRepository extends JpaRepository<NguoiThanDaLuu, Long> {

    /** Người thân đã lưu của tài khoản, người vừa đặt lịch gần nhất đứng trước. */
    List<NguoiThanDaLuu> findByTaiKhoanIdOrderByLanDungCuoiDescIdDesc(Long taiKhoanId);

    Optional<NguoiThanDaLuu> findByTaiKhoanIdAndHoSoBenhNhanId(Long taiKhoanId, Long hoSoBenhNhanId);

    /** Dòng của đúng tài khoản này; dòng của tài khoản khác coi như không có. */
    Optional<NguoiThanDaLuu> findByIdAndTaiKhoanId(Long id, Long taiKhoanId);

}
