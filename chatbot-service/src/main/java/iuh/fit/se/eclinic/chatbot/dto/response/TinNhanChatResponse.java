package iuh.fit.se.eclinic.chatbot.dto.response;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.LoaiTinNhanChat;
import iuh.fit.se.eclinic.common.enums.NguoiGui;

/**
 * 1 tin nhắn trong phiên chat.
 *
 * @param loai    CAU_HOI với tin của người dùng; với tin của chatbot: LOI_CHAO, TRA_LOI, NGOAI_PHAM_VI, KHAN_CAP
 * @param noiDung như đã lưu lúc gửi / trả lời, không đổi theo ngôn ngữ của request đọc lại
 * @param goiY    chỉ có ở TRA_LOI mà chatbot đã tra cứu dữ liệu phòng khám: chuyên khoa, bác sĩ, lịch khám còn chỗ; null
 *                khi không có
 */
public record TinNhanChatResponse(Long id, NguoiGui nguoiGui, LoaiTinNhanChat loai, String noiDung,
        LocalDateTime ngayTao, GoiYResponse goiY) {
}
