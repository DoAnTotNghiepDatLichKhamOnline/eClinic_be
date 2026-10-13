-- Chatbot trả lời về lịch hẹn và lượt khám của chính người dùng (UC-CHAT-01).
-- phien_chat.dong_y_du_lieu_luc: thời điểm bệnh nhân đồng ý cho chatbot đọc lịch hẹn, lượt khám của mình trong phiên này
-- và gửi phần tối thiểu (ngày giờ, bác sĩ, chuyên khoa, phòng, trạng thái, ngày tái khám) tới nhà cung cấp mô hình.
-- NULL = chưa đồng ý hoặc đã rút lại: không đọc, không gửi gì. Đồng ý tính theo từng phiên chat.
ALTER TABLE phien_chat
    ADD COLUMN dong_y_du_lieu_luc DATETIME NULL COMMENT 'lúc bệnh nhân đồng ý cho chatbot dùng lịch hẹn của mình; NULL = chưa' AFTER trang_thai;
