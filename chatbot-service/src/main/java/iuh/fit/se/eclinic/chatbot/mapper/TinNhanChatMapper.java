package iuh.fit.se.eclinic.chatbot.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.chatbot.dto.response.GoiYResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TinNhanChatResponse;
import iuh.fit.se.eclinic.common.entity.chatbot.TinNhanChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TinNhanChatMapper {

    private final JsonMapper jsonMapper;

    public TinNhanChatResponse toResponse(TinNhanChat tinNhan) {
        return new TinNhanChatResponse(tinNhan.getId(), tinNhan.getNguoiGui(), tinNhan.getLoai(),
                tinNhan.getNoiDung(), tinNhan.getNgayTao(), docGoiY(tinNhan));
    }

    public String ghiGoiY(GoiYResponse goiY) {
        return goiY == null ? null : jsonMapper.writeValueAsString(goiY);
    }

    /** null khi tin nhắn không có gợi ý; JSON hỏng thì coi như không có, không làm hỏng cả phiên. */
    public GoiYResponse docGoiY(TinNhanChat tinNhan) {
        if (tinNhan.getGoiY() == null) {
            return null;
        }
        try {
            return jsonMapper.readValue(tinNhan.getGoiY(), GoiYResponse.class);
        } catch (JacksonException ex) {
            log.warn("Không đọc được goi_y của tin nhắn {}", tinNhan.getId());
            return null;
        }
    }

}
