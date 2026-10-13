package iuh.fit.se.eclinic.chatbot.service;

import java.util.List;

import iuh.fit.se.eclinic.chatbot.dto.response.PhienChatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.PhienChatTomTatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TinNhanChatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TraLoiResponse;

/**
 * UC-CHAT-01: hội thoại với chatbot. Khách và người đã đăng nhập (mọi vai trò) đều dùng được.
 * <p>
 * Chủ của 1 phiên: tài khoản đã mở nó (lấy từ JWT), hoặc người cầm mã bí mật server cấp khi khách mở phiên. Phiên không
 * tồn tại và phiên của người khác trả cùng 1 lỗi 404; chỉ biết id phiên thì không đọc, không gửi được gì.
 */
public interface ChatbotService {

    /** Mở phiên mới kèm câu chào. Người gọi đã đăng nhập thì phiên thuộc tài khoản, không thì cấp mã bí mật. */
    PhienChatResponse moPhien(String diaChiIp);

    /**
     * Gửi 1 tin nhắn và nhận câu trả lời. Câu hỏi và câu trả lời được lưu cùng lúc; mô hình không trả lời được thì ném
     * CHATBOT_KHONG_KHA_DUNG và không lưu gì, người dùng gửi lại được.
     *
     * @param maKhach mã bí mật của phiên khách (header X-Chat-Khach); null với phiên của tài khoản
     */
    TraLoiResponse guiTinNhan(Long idPhien, String maKhach, String noiDung, String diaChiIp);

    List<TinNhanChatResponse> lichSu(Long idPhien, String maKhach);

    /**
     * Bệnh nhân đồng ý (hoặc rút lại) cho chatbot đọc lịch hẹn, lượt khám của chính mình trong phiên này và gửi phần tối
     * thiểu tới nhà cung cấp mô hình. Chỉ phiên của chính tài khoản đang đăng nhập; phiên khác, kể cả phiên của khách, ra
     * 404.
     */
    void datDongY(Long idPhien, boolean dongY);

    /** Các phiên chat gần nhất của tài khoản đang đăng nhập, mới nhất trước. */
    List<PhienChatTomTatResponse> phienCuaToi();

}
