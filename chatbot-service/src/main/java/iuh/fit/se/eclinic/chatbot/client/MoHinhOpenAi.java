package iuh.fit.se.eclinic.chatbot.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import iuh.fit.se.eclinic.chatbot.client.PhanHoiMoHinh.GoiCongCu;
import iuh.fit.se.eclinic.chatbot.config.ChatbotProperties;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.extern.slf4j.Slf4j;

/** Gọi mô hình qua API kiểu OpenAI ({@code POST <url-goc>/chat/completions}, khoá Bearer). */
@Slf4j
@Component
public class MoHinhOpenAi implements MoHinhNgonNgu {

    private static final Duration CHO_KET_NOI_TOI_DA = Duration.ofSeconds(5);
    private static final ParameterizedTypeReference<Map<String, Object>> KIEU_JSON = new ParameterizedTypeReference<>() {
    };

    private final ChatbotProperties properties;
    private final String urlGoi;

    public MoHinhOpenAi(ChatbotProperties properties) {
        this.properties = properties;
        this.urlGoi = properties.urlGoc().replaceAll("/+$", "") + "/chat/completions";
        if (!properties.daCauHinh()) {
            log.warn("Chưa đặt CHATBOT_API_KEY: chatbot tắt, API gửi tin nhắn sẽ trả 503.");
        }
    }

    @Override
    public PhanHoiMoHinh traLoi(String loiNhacHeThong, List<LuotChat> hoiThoai, List<CongCu> congCu,
            boolean duocTraCuu, Duration choToiDa) {
        if (!properties.daCauHinh()) {
            throw new LoiNghiepVu(MaLoi.CHATBOT_KHONG_KHA_DUNG);
        }
        List<Map<String, Object>> tinNhan = new ArrayList<>();
        tinNhan.add(Map.of("role", "system", "content", loiNhacHeThong));
        for (LuotChat luot : hoiThoai) {
            tinNhan.add(switch (luot.loai()) {
                case NGUOI_DUNG -> Map.of("role", "user", "content", luot.noiDung());
                case CHATBOT -> Map.of("role", "assistant", "content", luot.noiDung());
                case XIN_TRA_CUU -> luot.tinNhanGoc();
                case KET_QUA_TRA_CUU -> Map.of("role", "tool", "tool_call_id", luot.idGoiCongCu(), "content",
                        luot.noiDung());
            });
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.moHinh());
        body.put("messages", tinNhan);
        body.put("max_tokens", properties.soTokenTraLoiToiDa());
        if (properties.mucSuyLuan() != null && !properties.mucSuyLuan().isBlank()) {
            body.put("reasoning_effort", properties.mucSuyLuan().trim());
        }
        if (!congCu.isEmpty()) {
            body.put("tools", congCu.stream().map(MoHinhOpenAi::dinhNghia).toList());
            body.put("tool_choice", duocTraCuu ? "auto" : "none");
        }

        Map<String, Object> ketQua;
        try {
            // RestClient tạo cho từng lần gọi vì thời gian chờ thay đổi theo thời gian còn lại của tin nhắn
            ketQua = RestClient.builder().requestFactory(taoRequestFactory(choToiDa)).build().post()
                    .uri(urlGoi)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.khoaApi())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(KIEU_JSON);
        } catch (RestClientResponseException ex) {
            log.warn("Mô hình ngôn ngữ trả lỗi HTTP {}", ex.getStatusCode().value());
            throw new LoiNghiepVu(MaLoi.CHATBOT_KHONG_KHA_DUNG);
        } catch (RestClientException ex) {
            // Không kết nối được, quá thời gian chờ, response không đọc được
            Throwable goc = ex.getMostSpecificCause();
            log.warn("Không gọi được mô hình ngôn ngữ: {}", goc.getClass().getSimpleName());
            throw new LoiNghiepVu(MaLoi.CHATBOT_KHONG_KHA_DUNG);
        }
        PhanHoiMoHinh phanHoi = doc(ketQua);
        if (phanHoi == null || (phanHoi.goiCongCu().isEmpty() && !phanHoi.coNoiDung())) {
            log.warn("Mô hình ngôn ngữ trả lời rỗng");
            throw new LoiNghiepVu(MaLoi.CHATBOT_KHONG_KHA_DUNG);
        }
        return phanHoi;
    }

    private static Map<String, Object> dinhNghia(CongCu congCu) {
        return Map.of("type", "function", "function",
                Map.of("name", congCu.ten(), "description", congCu.moTa(), "parameters", congCu.thamSo()));
    }

    /** {@code choices[0].message} gồm content và tool_calls; null khi response không có dạng đó. */
    @SuppressWarnings("unchecked")
    private static PhanHoiMoHinh doc(Map<String, Object> ketQua) {
        if (ketQua == null || !(ketQua.get("choices") instanceof List<?> luaChon) || luaChon.isEmpty()
                || !(luaChon.get(0) instanceof Map<?, ?> dau) || !(dau.get("message") instanceof Map<?, ?> tinNhan)) {
            return null;
        }
        List<GoiCongCu> goiCongCu = new ArrayList<>();
        if (tinNhan.get("tool_calls") instanceof List<?> cacLoiGoi) {
            for (Object loiGoi : cacLoiGoi) {
                if (loiGoi instanceof Map<?, ?> mot && mot.get("function") instanceof Map<?, ?> ham
                        && ham.get("name") instanceof String ten) {
                    String id = mot.get("id") instanceof String chuoi ? chuoi : "";
                    String thamSo = ham.get("arguments") instanceof String chuoi ? chuoi : "";
                    goiCongCu.add(new GoiCongCu(id, ten, thamSo));
                }
            }
        }
        String noiDung = tinNhan.get("content") instanceof String chuoi ? chuoi : null;
        return new PhanHoiMoHinh(noiDung, goiCongCu, (Map<String, Object>) tinNhan);
    }

    private static SimpleClientHttpRequestFactory taoRequestFactory(Duration choToiDa) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(choToiDa.compareTo(CHO_KET_NOI_TOI_DA) > 0 ? CHO_KET_NOI_TOI_DA : choToiDa);
        factory.setReadTimeout(choToiDa);
        return factory;
    }

}
