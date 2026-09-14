package iuh.fit.fe.be_websatlichkham.modules.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.modules.user.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    /** Tìm hồ sơ khách vãng lai (chưa có tài khoản) theo SĐT, dùng khi lễ tân đặt hộ. */
    List<Patient> findByUserIsNullAndPhone(String phone);

}
