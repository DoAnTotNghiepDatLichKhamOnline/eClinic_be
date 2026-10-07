package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;

/**
 * CHỈ ĐỌC quản trị viên (bảng của identity-service) để ghi ai xếp ca, ai duyệt yêu cầu đổi lịch. Kế thừa
 * {@link Repository} chứ không phải JpaRepository nên không có save / delete.
 */
public interface QuanTriVienChiDocRepository extends Repository<QuanTriVien, Long> {

    Optional<QuanTriVien> findByTaiKhoanId(Long taiKhoanId);

}
