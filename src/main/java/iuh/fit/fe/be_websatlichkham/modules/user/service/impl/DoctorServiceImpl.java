package iuh.fit.fe.be_websatlichkham.modules.user.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Doctor;
import iuh.fit.fe.be_websatlichkham.modules.user.repository.DoctorRepository;
import iuh.fit.fe.be_websatlichkham.modules.user.service.DoctorService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    @Override
    public Doctor getById(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor", id));
    }

    @Override
    public Optional<Doctor> findByUserId(Long userId) {
        return doctorRepository.findByUserId(userId);
    }

    @Override
    public List<Doctor> findBySpecialization(Long specializationId) {
        return doctorRepository.findBySpecializationId(specializationId);
    }

}
