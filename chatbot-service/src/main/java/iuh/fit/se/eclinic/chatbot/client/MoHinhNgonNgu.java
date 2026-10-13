package iuh.fit.se.eclinic.chatbot.client;

import java.time.Duration;
import java.util.List;

/**
 * Mô hình ngôn ngữ trả lời hội thoại. Chỉ là 1 bước trong chuỗi xử lý tin nhắn của {@code ChatbotServiceImpl}: quyền
 * truy cập, giới hạn, tình huống cấp cứu và câu từ chối đều do code quyết định, không do mô hình. Mô hình không tự tra
 * cứu được gì: nó chỉ XIN gọi 1 trong các {@link CongCu} được đưa, code kiểm tra tham số rồi mới chạy.
 */
public interface MoHinhNgonNgu {

    /**
     * @param loiNhacHeThong chỉ dẫn của hệ thống (phạm vi, điều cấm, ngôn ngữ, danh sách chuyên khoa)
     * @param hoiThoai       các lượt cũ của phiên, câu hỏi mới, rồi các lượt xin tra cứu / kết quả tra cứu của tin nhắn này
     * @param congCu         các hàm tra cứu mô hình được xin gọi
     * @param duocTraCuu     false: mô hình phải trả lời luôn, không được xin tra cứu nữa (lần gọi cuối của 1 tin nhắn)
     * @param choToiDa       thời gian chờ của lần gọi này
     * @return câu trả lời không rỗng, hoặc ít nhất 1 lời gọi công cụ
     * @throws iuh.fit.se.eclinic.common.exception.LoiNghiepVu CHATBOT_KHONG_KHA_DUNG khi chưa đặt khoá API, mô hình
     *         không trả lời trong thời gian chờ, trả lỗi hoặc trả lời rỗng
     */
    PhanHoiMoHinh traLoi(String loiNhacHeThong, List<LuotChat> hoiThoai, List<CongCu> congCu, boolean duocTraCuu,
            Duration choToiDa);

}
