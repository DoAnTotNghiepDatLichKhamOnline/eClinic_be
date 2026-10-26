package iuh.fit.se.eclinic.chatbot.client;

import java.util.Map;

/** 1 lượt trong hội thoại gửi cho mô hình. */
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
