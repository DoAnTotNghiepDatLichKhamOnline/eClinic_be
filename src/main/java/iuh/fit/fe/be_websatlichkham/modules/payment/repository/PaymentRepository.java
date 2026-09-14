package iuh.fit.fe.be_websatlichkham.modules.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.modules.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

}
