package iuh.fit.fe.be_websatlichkham.modules.appointment.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.appointment.entity.Appointment;
import iuh.fit.fe.be_websatlichkham.modules.appointment.enums.AppointmentStatus;
import iuh.fit.fe.be_websatlichkham.modules.appointment.repository.AppointmentRepository;
import iuh.fit.fe.be_websatlichkham.modules.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Override
    public Appointment getById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }

    @Override
    public List<Appointment> findByPatient(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Override
    public List<Appointment> findByDoctorAndStatus(Long doctorId, AppointmentStatus status) {
        return appointmentRepository.findByDoctorIdAndStatusOrderByCreatedAtDesc(doctorId, status);
    }

}
