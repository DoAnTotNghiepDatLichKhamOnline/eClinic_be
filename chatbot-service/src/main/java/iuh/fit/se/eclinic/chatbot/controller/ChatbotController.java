package iuh.fit.se.eclinic.chatbot.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.chatbot.dto.request.GuiTinNhanRequest;
import iuh.fit.se.eclinic.chatbot.dto.response.PhienChatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.PhienChatTomTatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TinNhanChatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TraLoiResponse;
import iuh.fit.se.eclinic.chatbot.service.ChatbotService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.util.DiaChiIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Hội thoại với chatbot (trợ lý AI). */
@Tag(name = "Chatbot", description = "UC-CHAT-01: mở phiên chat, gửi tin nhắn, đọc lại hội thoại")
@RestController
@RequestMapping("/api/chatbot/phien")
@RequiredArgsConstructor
public class ChatbotController {

    public static final String HEADER_KHACH = "X-Chat-Khach";

    private final ChatbotService chatbotService;

    @Operation(summary = "Mở phiên chat mới, trả câu chào của chatbot. Chưa đăng nhập: trả thêm maKhach (chỉ 1 lần), gửi"
            + " lại ở header X-Chat-Khach trong các request sau của phiên; số phiên mở trong 1 giờ bị giới hạn")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<PhienChatResponse> moPhien(HttpServletRequest httpRequest) {
        return PhanHoiApi.ok(chatbotService.moPhien(DiaChiIp.cua(httpRequest)));
    }

    @Operation(summary = "Gửi 1 tin nhắn (tối đa 1000 ký tự), trả tin đã lưu và câu trả lời. loai của câu trả lời: TRA_LOI,"
            + " NGOAI_PHAM_VI (câu hỏi ngoài phạm vi), KHAN_CAP (dấu hiệu cấp cứu: gọi 115), CAN_DONG_Y (bệnh nhân phải đồng ý"
            + " trước khi chatbot đọc lịch hẹn của họ). 429 khi gửi quá nhiều; 503 khi chatbot không trả lời được (không lưu"
            + " gì, gửi lại được)")
    @PostMapping("/{idPhien}/tin-nhan")
    public PhanHoiApi<TraLoiResponse> guiTinNhan(@PathVariable Long idPhien,
            @RequestHeader(name = HEADER_KHACH, required = false) String maKhach,
            @Valid @RequestBody GuiTinNhanRequest request, HttpServletRequest httpRequest) {
        return PhanHoiApi.ok(chatbotService.guiTinNhan(idPhien, maKhach, request.noiDung(),
                DiaChiIp.cua(httpRequest)));
    }

    @Operation(summary = "Toàn bộ tin nhắn của 1 phiên của tôi, cũ nhất trước")
    @GetMapping("/{idPhien}/tin-nhan")
    public PhanHoiApi<List<TinNhanChatResponse>> lichSu(@PathVariable Long idPhien,
            @RequestHeader(name = HEADER_KHACH, required = false) String maKhach) {
        return PhanHoiApi.ok(chatbotService.lichSu(idPhien, maKhach));
    }

    @Operation(summary = "Bệnh nhân đồng ý cho chatbot đọc lịch hẹn, lượt khám của CHÍNH MÌNH trong phiên này (gọi sau khi"
            + " nhận câu trả lời loai CAN_DONG_Y, rồi gửi lại câu hỏi). Chỉ ngày giờ, bác sĩ, chuyên khoa, phòng, trạng thái"
            + " và ngày tái khám được gửi cho mô hình. Chỉ phiên của chính tài khoản")
    @PostMapping("/{idPhien}/dong-y")
    @PreAuthorize("hasRole('BENH_NHAN')")
    public PhanHoiApi<Void> dongY(@PathVariable Long idPhien) {
        chatbotService.datDongY(idPhien, true);
        return PhanHoiApi.ok(null, "thanhCong.dongYChat");
    }

    @Operation(summary = "Rút lại đồng ý của phiên: chatbot thôi đọc lịch hẹn của bệnh nhân và thôi gửi lại các câu trả lời"
            + " cũ về lịch hẹn cho mô hình")
    @DeleteMapping("/{idPhien}/dong-y")
    @PreAuthorize("hasRole('BENH_NHAN')")
    public PhanHoiApi<Void> rutDongY(@PathVariable Long idPhien) {
        chatbotService.datDongY(idPhien, false);
        return PhanHoiApi.ok(null, "thanhCong.rutDongYChat");
    }

    @Operation(summary = "Các phiên chat gần nhất của tài khoản đang đăng nhập (mới nhất trước, tối đa 50)")
    @GetMapping
    public PhanHoiApi<List<PhienChatTomTatResponse>> phienCuaToi() {
        return PhanHoiApi.ok(chatbotService.phienCuaToi());
    }

}
