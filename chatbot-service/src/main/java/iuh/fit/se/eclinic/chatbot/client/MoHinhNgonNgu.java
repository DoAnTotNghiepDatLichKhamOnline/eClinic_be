package iuh.fit.se.eclinic.chatbot.client;

import java.time.Duration;
import java.util.List;

/** Mô hình ngôn ngữ trả lời hội thoại. */
public interface MoHinhNgonNgu {

    PhanHoiMoHinh traLoi(String loiNhacHeThong, List<LuotChat> hoiThoai, List<CongCu> congCu, boolean duocTraCuu,
            Duration choToiDa);

}
