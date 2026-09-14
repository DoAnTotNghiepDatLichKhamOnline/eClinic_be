package iuh.fit.fe.be_websatlichkham.modules.appointment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.modules.appointment.entity.Appointment;
import iuh.fit.fe.be_websatlichkham.modules.appointment.enums.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Appointment> findByDoctorIdAndStatusOrderByCreatedAtDesc(Long doctorId, AppointmentStatus status);

    boolean existsBySlotIdAndStatusIn(Long slotId, List<AppointmentStatus> statuses);

}
