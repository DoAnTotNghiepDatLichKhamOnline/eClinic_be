package iuh.fit.fe.be_websatlichkham;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import iuh.fit.fe.be_websatlichkham.common.exception.BusinessException;
import iuh.fit.fe.be_websatlichkham.common.exception.ErrorCode;
import iuh.fit.fe.be_websatlichkham.modules.appointment.entity.Appointment;
import iuh.fit.fe.be_websatlichkham.modules.appointment.enums.AppointmentStatus;
import iuh.fit.fe.be_websatlichkham.modules.appointment.repository.AppointmentRepository;
import iuh.fit.fe.be_websatlichkham.modules.auth.enums.OtpPurpose;
import iuh.fit.fe.be_websatlichkham.modules.auth.service.OtpService;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Location;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Room;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Specialization;
import iuh.fit.fe.be_websatlichkham.modules.clinic.repository.LocationRepository;
import iuh.fit.fe.be_websatlichkham.modules.clinic.repository.RoomRepository;
import iuh.fit.fe.be_websatlichkham.modules.clinic.repository.SpecializationRepository;
import iuh.fit.fe.be_websatlichkham.modules.medical.entity.Medication;
import iuh.fit.fe.be_websatlichkham.modules.medical.service.MedicationService;
import iuh.fit.fe.be_websatlichkham.modules.payment.entity.Payment;
import iuh.fit.fe.be_websatlichkham.modules.payment.enums.PaymentMethod;
import iuh.fit.fe.be_websatlichkham.modules.payment.enums.PaymentStatus;
import iuh.fit.fe.be_websatlichkham.modules.payment.repository.PaymentRepository;
import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.TimeSlot;
import iuh.fit.fe.be_websatlichkham.modules.schedule.entity.WorkSchedule;
import iuh.fit.fe.be_websatlichkham.modules.schedule.repository.TimeSlotRepository;
import iuh.fit.fe.be_websatlichkham.modules.schedule.repository.WorkScheduleRepository;
import iuh.fit.fe.be_websatlichkham.modules.schedule.service.WorkScheduleService;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Doctor;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Patient;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.User;
import iuh.fit.fe.be_websatlichkham.modules.user.enums.UserRole;
import iuh.fit.fe.be_websatlichkham.modules.user.repository.DoctorRepository;
import iuh.fit.fe.be_websatlichkham.modules.user.repository.PatientRepository;
import iuh.fit.fe.be_websatlichkham.modules.user.repository.UserRepository;

/**
 * Kiểm tra các ràng buộc toàn vẹn dữ liệu được đặt ở mức DB / service.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaConstraintsTest {

    private static final LocalDate WORK_DATE = LocalDate.of(2030, 1, 15);

    @Autowired UserRepository userRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired SpecializationRepository specializationRepository;
    @Autowired LocationRepository locationRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired WorkScheduleRepository workScheduleRepository;
    @Autowired TimeSlotRepository timeSlotRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired WorkScheduleService workScheduleService;
    @Autowired MedicationService medicationService;
    @Autowired OtpService otpService;

    @Test
    void activeAppointmentsCannotShareASlot_butCancelledSlotCanBeRebooked() {
        Fixture f = createFixture();

        Appointment first = appointmentRepository.saveAndFlush(newAppointment(f));

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(newAppointment(f)))
                .isInstanceOf(DataIntegrityViolationException.class);

        first.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.saveAndFlush(first);

        assertThatCode(() -> appointmentRepository.saveAndFlush(newAppointment(f))).doesNotThrowAnyException();
    }

    @Test
    void appointmentCanHaveOnlyOnePaidPayment() {
        Fixture f = createFixture();
        Appointment appointment = appointmentRepository.saveAndFlush(newAppointment(f));

        paymentRepository.saveAndFlush(newPayment(appointment, PaymentStatus.FAILED));
        paymentRepository.saveAndFlush(newPayment(appointment, PaymentStatus.PAID));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(newPayment(appointment, PaymentStatus.PAID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guestPatientRequiresNameAndPhone() {
        Patient guestWithoutInfo = new Patient();

        assertThatThrownBy(() -> patientRepository.saveAndFlush(guestWithoutInfo))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void overlappingWorkScheduleIsRejected() {
        Fixture f = createFixture();

        assertThatThrownBy(() -> workScheduleService.assertNoOverlap(f.doctor().getId(), f.room().getId(), WORK_DATE,
                LocalTime.of(9, 0), LocalTime.of(10, 0), null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_OVERLAP);

        assertThatCode(() -> workScheduleService.assertNoOverlap(f.doctor().getId(), f.room().getId(), WORK_DATE,
                LocalTime.of(12, 0), LocalTime.of(13, 0), null)).doesNotThrowAnyException();
    }

    @Test
    void medicationGetOrCreateIsCaseAndWhitespaceInsensitive() {
        String name = "Paracetamol " + UUID.randomUUID().toString().substring(0, 8);

        Medication created = medicationService.getOrCreate("  " + name + "  ", "viên", null);
        Medication again = medicationService.getOrCreate(name.toUpperCase(), "viên", null);

        assertThat(again.getId()).isEqualTo(created.getId());
        assertThat(created.getName()).isEqualTo(name);
        assertThat(created.isVerified()).isFalse();
    }

    @Test
    void otpCanBeVerifiedOnlyOnce() {
        long userId = System.nanoTime();
        String code = otpService.generate(userId, OtpPurpose.REGISTER);

        assertThatThrownBy(() -> otpService.verify(userId, OtpPurpose.REGISTER, "wrong"))
                .isInstanceOf(BusinessException.class);
        assertThatCode(() -> otpService.verify(userId, OtpPurpose.REGISTER, code)).doesNotThrowAnyException();
        assertThatThrownBy(() -> otpService.verify(userId, OtpPurpose.REGISTER, code))
                .isInstanceOf(BusinessException.class);
    }

    // ---------------------------------------------------------------- fixtures

    private record Fixture(Patient patient, Doctor doctor, Location location, Room room, TimeSlot slot) {
    }

    private Fixture createFixture() {
        String suffix = UUID.randomUUID().toString();

        User doctorUser = new User();
        doctorUser.setEmail("doctor-" + suffix + "@test.local");
        doctorUser.setPasswordHash("x");
        doctorUser.setRole(UserRole.DOCTOR);
        doctorUser = userRepository.save(doctorUser);

        Specialization specialization = new Specialization();
        specialization.setName("Spec " + suffix);
        specialization = specializationRepository.save(specialization);

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization(specialization);
        doctor.setConsultationFee(new BigDecimal("200000"));
        doctor = doctorRepository.save(doctor);

        Location location = new Location();
        location.setName("Location " + suffix);
        location = locationRepository.save(location);

        Room room = new Room();
        room.setLocation(location);
        room.setSpecialization(specialization);
        room.setRoomName("Room " + suffix);
        room = roomRepository.save(room);

        WorkSchedule schedule = new WorkSchedule();
        schedule.setDoctor(doctor);
        schedule.setRoom(room);
        schedule.setWorkDate(WORK_DATE);
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(11, 0));
        schedule = workScheduleRepository.save(schedule);

        TimeSlot slot = new TimeSlot();
        slot.setSchedule(schedule);
        slot.setStartTime(WORK_DATE.atTime(8, 0));
        slot.setEndTime(WORK_DATE.atTime(8, 30));
        slot = timeSlotRepository.save(slot);

        Patient guest = new Patient();
        guest.setFullName("Khách vãng lai");
        guest.setPhone("0900000000");
        guest = patientRepository.save(guest);

        return new Fixture(guest, doctor, location, room, slot);
    }

    private static Appointment newAppointment(Fixture f) {
        Appointment appointment = new Appointment();
        appointment.setPatient(f.patient());
        appointment.setDoctor(f.doctor());
        appointment.setSlot(f.slot());
        appointment.setLocation(f.location());
        return appointment;
    }

    private static Payment newPayment(Appointment appointment, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(new BigDecimal("200000"));
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(status);
        return payment;
    }

}
