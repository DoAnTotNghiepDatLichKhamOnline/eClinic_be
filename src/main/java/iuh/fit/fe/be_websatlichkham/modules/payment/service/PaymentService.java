package iuh.fit.fe.be_websatlichkham.modules.payment.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.payment.entity.Payment;

public interface PaymentService {

    Payment getById(Long id);

    /** Lịch sử thanh toán của 1 lịch hẹn, mới nhất trước. */
    List<Payment> findByAppointment(Long appointmentId);

}
