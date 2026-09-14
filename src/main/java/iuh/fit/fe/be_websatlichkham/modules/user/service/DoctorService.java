package iuh.fit.fe.be_websatlichkham.modules.user.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.modules.user.entity.Doctor;

public interface DoctorService {

    Doctor getById(Long id);

    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findBySpecialization(Long specializationId);

}
