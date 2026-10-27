package iuh.fit.se.eclinic.chatbot.client;

import java.util.List;
import java.util.Map;

/** 1 lần trả lời của mô hình: hoặc là câu trả lời ({@code goiCongCu} rỗng), hoặc là yêu cầu tra cứu. */
public record PhanHoiMoHinh(String noiDung, List<GoiCongCu> goiCongCu, Map<String, Object> tinNhanGoc) {

    public record GoiCongCu(String id, String ten, String thamSo) {
    }

    public boolean coNoiDung() {
        return noiDung != null && !noiDung.isBlank();
    }

}
