package iuh.fit.fe.be_websatlichkham.modules.user.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.modules.user.entity.Patient;

public interface PatientService {

    Patient getById(Long id);

    Optional<Patient> findByUserId(Long userId);

    /** Hồ sơ khách vãng lai (chưa có tài khoản) có SĐT này. */
    List<Patient> findGuestsByPhone(String phone);

}
