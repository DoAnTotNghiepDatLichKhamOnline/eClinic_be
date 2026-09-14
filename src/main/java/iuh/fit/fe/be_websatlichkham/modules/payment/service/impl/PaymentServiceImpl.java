package iuh.fit.fe.be_websatlichkham.modules.payment.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.payment.entity.Payment;
import iuh.fit.fe.be_websatlichkham.modules.payment.repository.PaymentRepository;
import iuh.fit.fe.be_websatlichkham.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment getById(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    @Override
    public List<Payment> findByAppointment(Long appointmentId) {
        return paymentRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId);
    }

}
