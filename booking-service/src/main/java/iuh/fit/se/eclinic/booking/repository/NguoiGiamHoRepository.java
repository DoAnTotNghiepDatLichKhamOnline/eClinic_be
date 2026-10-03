package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.booking.NguoiGiamHo;

public interface NguoiGiamHoRepository extends JpaRepository<NguoiGiamHo, Long> {

    /** Người giám hộ đã khai cho hồ sơ này theo CCCD (mỗi hồ sơ + CCCD chỉ lưu 1 dòng). */
    Optional<NguoiGiamHo> findByHoSoBenhNhanIdAndCccd(Long hoSoBenhNhanId, String cccd);

}
