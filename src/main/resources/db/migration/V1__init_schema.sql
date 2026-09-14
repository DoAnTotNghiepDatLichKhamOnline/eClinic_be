-- =====================================================================
-- eClinic - Hệ thống đặt lịch khám bệnh (schema v3, đã tinh chỉnh)
-- Flyway migration V1: tạo toàn bộ bảng, khoá ngoại, index, ràng buộc.
--
-- QUY ƯỚC
--  * InnoDB + utf8mb4, id BIGINT AUTO_INCREMENT.
--  * Enum lưu dạng VARCHAR (Java: @Enumerated(STRING)); giá trị hợp lệ ghi ở COMMENT.
--  * Khoá ngoại mặc định RESTRICT (không cho xoá cha khi còn con).
--    Chỉ CASCADE với bảng con thuần tuý: refresh_tokens, prescription_items, chat_messages.
--  * KHÔNG sửa file này sau khi đã chạy trên máy ai đó -> tạo V2__..., V3__... mới.
--
-- THAY ĐỔI SO VỚI ERD v3
--  * Khách vãng lai = patients có user_id NULL (bỏ appointments.guest_id).
--  * appointments.active_slot_id (generated + UNIQUE): DB chặn 2 lịch hẹn còn hiệu lực trên cùng 1 slot.
--  * payments.paid_appointment_id (generated + UNIQUE): mỗi lịch hẹn tối đa 1 payment PAID.
--  * refresh_tokens lưu token_hash (SHA-256) thay vì token gốc.
--  * medications.normalized_name UNIQUE để get-or-create an toàn khi nhiều bác sĩ nhập cùng lúc.
-- =====================================================================

-- ---------- CỘT 1: TÀI KHOẢN GỐC ----------
CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(150),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    avatar_url    VARCHAR(500),
    role          VARCHAR(20)  NOT NULL COMMENT 'PATIENT | DOCTOR | ADMIN',
    status        VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION' COMMENT 'PENDING_VERIFICATION | ACTIVE | LOCKED',
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- ---------- CỘT 3 (tạo trước vì bảng vai trò tham chiếu tới) ----------
CREATE TABLE specializations
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_specializations PRIMARY KEY (id),
    CONSTRAINT uk_specializations_name UNIQUE (name)
);

CREATE TABLE locations
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(200) NOT NULL,
    address     VARCHAR(500),
    city        VARCHAR(100),
    phone       VARCHAR(20),
    description TEXT,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_locations PRIMARY KEY (id)
);

-- ---------- CỘT 2: VAI TRÒ CỤ THỂ ----------
CREATE TABLE admins
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    permission_level    VARCHAR(20) NOT NULL DEFAULT 'STAFF' COMMENT 'SUPER_ADMIN | STAFF',
    managed_location_id BIGINT COMMENT 'NULL = quản lý toàn hệ thống',
    CONSTRAINT pk_admins PRIMARY KEY (id),
    CONSTRAINT uk_admins_user UNIQUE (user_id),
    CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_admins_location FOREIGN KEY (managed_location_id) REFERENCES locations (id)
);

CREATE TABLE patients
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT COMMENT 'NULL = khách vãng lai (chưa có tài khoản)',
    full_name        VARCHAR(150) COMMENT 'Bắt buộc với khách vãng lai; với bệnh nhân có tài khoản lấy từ users',
    phone            VARCHAR(20) COMMENT 'Bắt buộc với khách vãng lai',
    email            VARCHAR(255),
    date_of_birth    DATE,
    gender           VARCHAR(10) COMMENT 'MALE | FEMALE | OTHER',
    address          VARCHAR(500),
    insurance_number VARCHAR(50),
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uk_patients_user UNIQUE (user_id),
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_patients_guest_info CHECK (user_id IS NOT NULL OR (full_name IS NOT NULL AND phone IS NOT NULL))
);
CREATE INDEX idx_patients_phone ON patients (phone);

CREATE TABLE doctors
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    user_id             BIGINT         NOT NULL,
    specialization_id   BIGINT         NOT NULL,
    license_no          VARCHAR(50),
    bio                 TEXT,
    years_of_experience INT,
    consultation_fee    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_doctors PRIMARY KEY (id),
    CONSTRAINT uk_doctors_user UNIQUE (user_id),
    CONSTRAINT uk_doctors_license UNIQUE (license_no),
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_doctors_specialization FOREIGN KEY (specialization_id) REFERENCES specializations (id),
    CONSTRAINT ck_doctors_fee CHECK (consultation_fee >= 0),
    CONSTRAINT ck_doctors_experience CHECK (years_of_experience IS NULL OR years_of_experience >= 0)
);

-- ---------- CỘT 3: PHÒNG KHÁM + XÁC THỰC ----------
CREATE TABLE rooms
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    location_id       BIGINT       NOT NULL,
    specialization_id BIGINT       NOT NULL,
    room_name         VARCHAR(100) NOT NULL COMMENT 'vd: Phòng 203',
    floor             VARCHAR(20),
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE',
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT uk_rooms_location_name UNIQUE (location_id, room_name),
    CONSTRAINT fk_rooms_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_rooms_specialization FOREIGN KEY (specialization_id) REFERENCES specializations (id)
);

CREATE TABLE refresh_tokens
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hex của refresh token, KHÔNG lưu token gốc',
    device_info  VARCHAR(255) COMMENT 'hiển thị danh sách phiên đăng nhập',
    ip_address   VARCHAR(45),
    expires_at   DATETIME(6)  NOT NULL,
    revoked_at   DATETIME(6) COMMENT 'NULL = còn hiệu lực',
    last_used_at DATETIME(6),
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- ---------- CỘT 4: CHỨC NĂNG CỦA DOCTOR ----------
CREATE TABLE work_schedules
(
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    doctor_id             BIGINT      NOT NULL,
    room_id               BIGINT      NOT NULL,
    work_date             DATE        NOT NULL,
    start_time            TIME        NOT NULL,
    end_time              TIME        NOT NULL,
    slot_duration_minutes INT         NOT NULL DEFAULT 30 COMMENT 'dùng để sinh time_slots',
    created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_work_schedules PRIMARY KEY (id),
    CONSTRAINT fk_work_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_work_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT ck_work_schedules_time CHECK (end_time > start_time),
    CONSTRAINT ck_work_schedules_duration CHECK (slot_duration_minutes > 0)
);
-- Chồng giờ (overlap) không kiểm được bằng UNIQUE -> kiểm ở WorkScheduleService.assertNoOverlap
CREATE INDEX idx_work_schedules_doctor_date ON work_schedules (doctor_id, work_date);
CREATE INDEX idx_work_schedules_room_date ON work_schedules (room_id, work_date);

CREATE TABLE time_slots
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT      NOT NULL,
    start_time  DATETIME(6) NOT NULL,
    end_time    DATETIME(6) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE | HELD | BOOKED | CANCELLED',
    held_until  DATETIME(6) COMMENT 'hết hạn giữ chỗ khi status = HELD',
    version     BIGINT      NOT NULL DEFAULT 0 COMMENT 'optimistic locking',
    CONSTRAINT pk_time_slots PRIMARY KEY (id),
    CONSTRAINT uk_time_slots_schedule_start UNIQUE (schedule_id, start_time),
    CONSTRAINT fk_time_slots_schedule FOREIGN KEY (schedule_id) REFERENCES work_schedules (id),
    CONSTRAINT ck_time_slots_time CHECK (end_time > start_time)
);
CREATE INDEX idx_time_slots_status_start ON time_slots (status, start_time);

-- ---------- CỘT 5: LỊCH HẸN + THANH TOÁN ----------
CREATE TABLE appointments
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    patient_id          BIGINT      NOT NULL COMMENT 'bệnh nhân có tài khoản hoặc khách vãng lai',
    doctor_id           BIGINT      NOT NULL,
    slot_id             BIGINT      NOT NULL,
    location_id         BIGINT      NOT NULL COMMENT 'denormalized từ slot->schedule->room->location',
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIRMATION'
        COMMENT 'PENDING_CONFIRMATION | CONFIRMED | CANCELLED_BY_RESCHEDULE | CANCELLED | REJECTED | COMPLETED',
    reason              VARCHAR(500),
    note                TEXT,
    cancellation_reason VARCHAR(500) COMMENT 'lý do huỷ / từ chối',
    rescheduled_from_id BIGINT COMMENT 'lịch hẹn cũ nếu lịch này được tạo do đổi lịch',
    created_by_user_id  BIGINT COMMENT 'người thao tác đặt (bệnh nhân hoặc lễ tân/admin đặt hộ)',
    version             BIGINT      NOT NULL DEFAULT 0 COMMENT 'optimistic locking',
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- Chỉ có giá trị khi lịch hẹn còn "chiếm" slot -> UNIQUE bên dưới chống trùng lịch ở mức DB.
    -- Lịch đã huỷ/từ chối/đổi lịch có active_slot_id = NULL nên slot được đặt lại bình thường.
    active_slot_id      BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'COMPLETED') THEN slot_id END
        ) STORED,
    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT uk_appointments_active_slot UNIQUE (active_slot_id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_appointments_slot FOREIGN KEY (slot_id) REFERENCES time_slots (id),
    CONSTRAINT fk_appointments_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT fk_appointments_rescheduled_from FOREIGN KEY (rescheduled_from_id) REFERENCES appointments (id),
    CONSTRAINT fk_appointments_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);
CREATE INDEX idx_appointments_patient_status ON appointments (patient_id, status);
CREATE INDEX idx_appointments_doctor_status ON appointments (doctor_id, status);
CREATE INDEX idx_appointments_slot ON appointments (slot_id);

CREATE TABLE payments
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    appointment_id      BIGINT         NOT NULL,
    amount              DECIMAL(12, 2) NOT NULL COMMENT 'copy doctors.consultation_fee tại thời điểm đặt',
    method              VARCHAR(20)    NOT NULL COMMENT 'CASH | BANK_TRANSFER | E_WALLET | CARD',
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | PAID | FAILED | REFUNDED',
    transaction_code    VARCHAR(100),
    paid_at             DATETIME(6),
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- Mỗi appointment tối đa 1 payment PAID (các lần FAILED/REFUNDED vẫn lưu lịch sử).
    paid_appointment_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PAID' THEN appointment_id END
        ) STORED,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_transaction_code UNIQUE (transaction_code),
    CONSTRAINT uk_payments_paid_appointment UNIQUE (paid_appointment_id),
    CONSTRAINT fk_payments_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT ck_payments_amount CHECK (amount >= 0)
);
CREATE INDEX idx_payments_appointment ON payments (appointment_id);

-- ---------- CỘT 6: HỒ SƠ BỆNH ÁN + TOA THUỐC + THÔNG BÁO ----------
CREATE TABLE medical_records
(
    id                        BIGINT      NOT NULL AUTO_INCREMENT,
    appointment_id            BIGINT      NOT NULL,
    patient_id                BIGINT      NOT NULL,
    doctor_id                 BIGINT      NOT NULL,
    diagnosis                 TEXT,
    notes                     TEXT,
    next_visit_suggested_date DATE COMMENT 'gợi ý tái khám, KHÔNG tự tạo appointment',
    created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_medical_records PRIMARY KEY (id),
    CONSTRAINT uk_medical_records_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_medical_records_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_medical_records_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_medical_records_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id)
);
CREATE INDEX idx_medical_records_patient ON medical_records (patient_id);

CREATE TABLE medications
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(255) NOT NULL COMMENT 'tên hiển thị như bác sĩ nhập',
    normalized_name      VARCHAR(255) NOT NULL COMMENT 'lowercase + trim + gộp khoảng trắng, dùng cho get-or-create',
    unit                 VARCHAR(30) COMMENT 'viên, ml, gói...',
    description          TEXT,
    is_verified          BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'false = bác sĩ tự thêm; true = admin đã duyệt',
    created_by_doctor_id BIGINT,
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_medications PRIMARY KEY (id),
    CONSTRAINT uk_medications_normalized_name UNIQUE (normalized_name),
    CONSTRAINT fk_medications_created_by FOREIGN KEY (created_by_doctor_id) REFERENCES doctors (id)
);

CREATE TABLE prescription_items
(
    id                BIGINT NOT NULL AUTO_INCREMENT,
    medical_record_id BIGINT NOT NULL,
    medication_id     BIGINT NOT NULL,
    dosage            VARCHAR(100) COMMENT 'vd: 500mg/lần',
    frequency_per_day INT COMMENT 'số lần uống trong ngày',
    duration_days     INT,
    usage_note        VARCHAR(255) COMMENT 'vd: uống sau ăn',
    CONSTRAINT pk_prescription_items PRIMARY KEY (id),
    CONSTRAINT fk_prescription_items_record FOREIGN KEY (medical_record_id) REFERENCES medical_records (id) ON DELETE CASCADE,
    CONSTRAINT fk_prescription_items_medication FOREIGN KEY (medication_id) REFERENCES medications (id),
    CONSTRAINT ck_prescription_items_frequency CHECK (frequency_per_day IS NULL OR frequency_per_day > 0),
    CONSTRAINT ck_prescription_items_duration CHECK (duration_days IS NULL OR duration_days > 0)
);
CREATE INDEX idx_prescription_items_record ON prescription_items (medical_record_id);

CREATE TABLE notifications
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    appointment_id BIGINT,
    title          VARCHAR(255),
    content        TEXT,
    type           VARCHAR(30) NOT NULL COMMENT 'REMINDER_APPOINTMENT | REMINDER_MEDICATION | PAYMENT | SYSTEM',
    is_read        BOOLEAN     NOT NULL DEFAULT FALSE,
    scheduled_at   DATETIME(6) COMMENT 'thời điểm cần gửi (nhắc lịch, nhắc tái khám); NULL = gửi ngay',
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id)
);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read, created_at);

-- ---------- CỘT 7: CHATBOT ----------
CREATE TABLE chat_sessions
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT COMMENT 'NULL nếu khách chưa đăng nhập',
    anonymous_key VARCHAR(64) COMMENT 'định danh phía client cho khách chưa đăng nhập',
    status        VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN | CLOSED',
    started_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at      DATETIME(6),
    CONSTRAINT pk_chat_sessions PRIMARY KEY (id),
    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_chat_sessions_user ON chat_sessions (user_id);
CREATE INDEX idx_chat_sessions_anonymous_key ON chat_sessions (anonymous_key);

CREATE TABLE chat_messages
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    session_id     BIGINT      NOT NULL,
    sender         VARCHAR(10) NOT NULL COMMENT 'USER | BOT | STAFF',
    sender_user_id BIGINT COMMENT 'nhân viên trả lời khi sender = STAFF',
    content        TEXT        NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender_user FOREIGN KEY (sender_user_id) REFERENCES users (id)
);
CREATE INDEX idx_chat_messages_session_created ON chat_messages (session_id, created_at);
