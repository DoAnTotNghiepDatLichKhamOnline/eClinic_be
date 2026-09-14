package iuh.fit.fe.be_websatlichkham.modules.appointment.enums;

/**
 * Trạng thái lịch hẹn.
 */
public enum AppointmentStatus {
    PENDING_CONFIRMATION,
    CONFIRMED,
    CANCELLED_BY_RESCHEDULE,
    CANCELLED,
    REJECTED,
    COMPLETED;

    /**
     * Lịch hẹn ở trạng thái này đang "chiếm" slot. Phải khớp với biểu thức của cột
     * {@code appointments.active_slot_id} trong V1__init_schema.sql.
     */
    public boolean occupiesSlot() {
        return this == PENDING_CONFIRMATION || this == CONFIRMED || this == COMPLETED;
    }
}
