package iuh.fit.se.eclinic.booking.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.request.KhachCapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.service.CapLaiLienKetService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.util.DiaChiIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Khách đã mất link phiếu khám tự xin cấp lại. */
@Tag(name = "Cấp lại liên kết", description = "PAT-03: khách mất link tự xin gửi lại liên kết phiếu khám qua email")
@RestController
@RequestMapping("/api/booking/cap-lai-lien-ket")
@RequiredArgsConstructor
public class CapLaiLienKetController {

    private final CapLaiLienKetService capLaiLienKetService;

    @Operation(summary = "Gửi lại liên kết phiếu khám (và sổ khám bệnh nếu đã khám) tới email của lịch hẹn khi maTraCuu +"
            + " soDienThoai (đầy đủ SĐT liên hệ lúc đặt; có người giám hộ: SĐT người giám hộ) khớp. Luôn 200 cùng 1 câu,"
            + " không bao giờ trả link. Mỗi mã tối đa 3 lần, mỗi IP 10 lần trong 1 giờ (đúng hay sai đều tính): vượt ->"
            + " 429 GUI_LAI_QUA_NHANH kèm số phút còn lại. Lịch hẹn đã đổi: gửi phiếu của lịch mới nhất")
    @PostMapping
    public PhanHoiApi<Void> capLai(@Valid @RequestBody KhachCapLaiLienKetRequest request,
            HttpServletRequest httpRequest) {
        capLaiLienKetService.khach(request, DiaChiIp.cua(httpRequest));
        return PhanHoiApi.ok(null, "thanhCong.capLaiLienKet.khach");
    }

}
