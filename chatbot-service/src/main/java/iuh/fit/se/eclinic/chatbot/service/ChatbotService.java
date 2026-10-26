package iuh.fit.se.eclinic.chatbot.service;

import java.util.List;

import iuh.fit.se.eclinic.chatbot.dto.response.PhienChatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.PhienChatTomTatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TinNhanChatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TraLoiResponse;

/** Hội thoại với chatbot. */
public interface ChatbotService {

    /** Mở phiên mới kèm câu chào. Người gọi đã đăng nhập thì phiên thuộc tài khoản, không thì cấp mã bí mật. */
    PhienChatResponse moPhien(String diaChiIp);

    /** Gửi 1 tin nhắn và nhận câu trả lời. */
    TraLoiResponse guiTinNhan(Long idPhien, String maKhach, String noiDung, String diaChiIp);

    List<TinNhanChatResponse> lichSu(Long idPhien, String maKhach);

    /** Bệnh nhân đồng ý hoặc rút lại quyền cho chatbot đọc lịch hẹn, lượt khám của mình trong phiên. */
    void datDongY(Long idPhien, boolean dongY);

    /** Các phiên chat gần nhất của tài khoản đang đăng nhập, mới nhất trước. */
    List<PhienChatTomTatResponse> phienCuaToi();

}
