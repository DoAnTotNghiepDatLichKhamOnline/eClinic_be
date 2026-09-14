package iuh.fit.fe.be_websatlichkham.modules.user.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Patient;
import iuh.fit.fe.be_websatlichkham.modules.user.repository.PatientRepository;
import iuh.fit.fe.be_websatlichkham.modules.user.service.PatientService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    public Patient getById(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }

    @Override
    public Optional<Patient> findByUserId(Long userId) {
        return patientRepository.findByUserId(userId);
    }

    @Override
    public List<Patient> findGuestsByPhone(String phone) {
        return patientRepository.findByUserIsNullAndPhone(phone);
    }

}
