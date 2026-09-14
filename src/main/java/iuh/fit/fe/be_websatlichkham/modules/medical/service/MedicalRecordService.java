package iuh.fit.fe.be_websatlichkham.modules.medical.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.modules.medical.entity.MedicalRecord;

public interface MedicalRecordService {

    MedicalRecord getById(Long id);

    Optional<MedicalRecord> findByAppointment(Long appointmentId);

    List<MedicalRecord> findByPatient(Long patientId);

}
