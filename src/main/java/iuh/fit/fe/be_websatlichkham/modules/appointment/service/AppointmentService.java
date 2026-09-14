package iuh.fit.fe.be_websatlichkham.modules.appointment.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.appointment.entity.Appointment;
import iuh.fit.fe.be_websatlichkham.modules.appointment.enums.AppointmentStatus;

public interface AppointmentService {

    Appointment getById(Long id);

    List<Appointment> findByPatient(Long patientId);

    List<Appointment> findByDoctorAndStatus(Long doctorId, AppointmentStatus status);

}
