package iuh.fit.fe.be_websatlichkham.modules.medical.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.medical.entity.MedicalRecord;
import iuh.fit.fe.be_websatlichkham.modules.medical.repository.MedicalRecordRepository;
import iuh.fit.fe.be_websatlichkham.modules.medical.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    @Override
    public MedicalRecord getById(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", id));
    }

    @Override
    public Optional<MedicalRecord> findByAppointment(Long appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId);
    }

    @Override
    public List<MedicalRecord> findByPatient(Long patientId) {
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

}
