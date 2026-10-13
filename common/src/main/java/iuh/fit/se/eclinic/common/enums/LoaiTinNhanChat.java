package iuh.fit.se.eclinic.common.enums;

/**
 * Loại tin nhắn trong phiên chat. Tin của người dùng luôn là {@link #CAU_HOI}; các loại còn lại là tin của chatbot.
 */
public enum LoaiTinNhanChat {
    CAU_HOI,
    /** Câu chào khi mở phiên: nói rõ đây là trợ lý AI, không thay bác sĩ. */
    LOI_CHAO,
    /** Câu trả lời của mô hình ngôn ngữ. */
    TRA_LOI,
    /** Câu từ chối cố định do server viết khi câu hỏi nằm ngoài phạm vi. */
    NGOAI_PHAM_VI,
    /** Câu "gọi 115" cố định khi tin nhắn có dấu hiệu cấp cứu; mô hình không được gọi. */
    KHAN_CAP,
    /**
     * Câu cố định xin bệnh nhân đồng ý trước khi chatbot đọc lịch hẹn, lượt khám của họ; chưa có dữ liệu nào được đọc.
     * Frontend hiện nút "Đồng ý" (POST /api/chatbot/phien/{id}/dong-y) rồi gửi lại câu hỏi.
     */
    CAN_DONG_Y
}
