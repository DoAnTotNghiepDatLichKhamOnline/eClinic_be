package iuh.fit.se.eclinic.chatbot.client;

import java.util.List;
import java.util.Map;

/**
 * 1 lần trả lời của mô hình: hoặc là câu trả lời ({@code goiCongCu} rỗng), hoặc là yêu cầu tra cứu.
 *
 * @param noiDung    câu trả lời; có thể null khi mô hình xin tra cứu
 * @param goiCongCu  các lời gọi công cụ mô hình xin, rỗng khi mô hình trả lời luôn
 * @param tinNhanGoc tin nhắn của mô hình đúng như đã nhận, để gửi lại nguyên vẹn ở lần gọi sau (nhà cung cấp có thể gắn
 *                   thêm trường riêng vào lời gọi công cụ và đòi nhận lại đủ)
 */
public record PhanHoiMoHinh(String noiDung, List<GoiCongCu> goiCongCu, Map<String, Object> tinNhanGoc) {

    /**
     * @param thamSo tham số dạng chuỗi JSON do mô hình viết: chưa được kiểm tra gì
     */
    public record GoiCongCu(String id, String ten, String thamSo) {
    }

    public boolean coNoiDung() {
        return noiDung != null && !noiDung.isBlank();
    }

}
