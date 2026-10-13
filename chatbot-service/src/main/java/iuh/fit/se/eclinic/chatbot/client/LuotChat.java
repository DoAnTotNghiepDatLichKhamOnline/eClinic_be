package iuh.fit.se.eclinic.chatbot.client;

import java.util.Map;

/**
 * 1 lượt trong hội thoại gửi cho mô hình.
 *
 * @param noiDung      lời của người dùng / của chatbot, hoặc kết quả tra cứu (JSON)
 * @param tinNhanGoc   chỉ với {@link Loai#XIN_TRA_CUU}: tin nhắn mô hình đã trả, giữ nguyên từng trường để gửi lại
 * @param idGoiCongCu  chỉ với {@link Loai#KET_QUA_TRA_CUU}: id của lời gọi công cụ mà kết quả này trả lời
 */
public record LuotChat(Loai loai, String noiDung, Map<String, Object> tinNhanGoc, String idGoiCongCu) {

    public enum Loai {
        NGUOI_DUNG,
        CHATBOT,
        /** Mô hình xin tra cứu (gọi công cụ) thay vì trả lời. */
        XIN_TRA_CUU,
        KET_QUA_TRA_CUU
    }

    public static LuotChat nguoiDung(String noiDung) {
        return new LuotChat(Loai.NGUOI_DUNG, noiDung, null, null);
    }

    public static LuotChat chatbot(String noiDung) {
        return new LuotChat(Loai.CHATBOT, noiDung, null, null);
    }

    public static LuotChat xinTraCuu(Map<String, Object> tinNhanGoc) {
        return new LuotChat(Loai.XIN_TRA_CUU, null, tinNhanGoc, null);
    }

    public static LuotChat ketQuaTraCuu(String idGoiCongCu, String ketQua) {
        return new LuotChat(Loai.KET_QUA_TRA_CUU, ketQua, null, idGoiCongCu);
    }

}
