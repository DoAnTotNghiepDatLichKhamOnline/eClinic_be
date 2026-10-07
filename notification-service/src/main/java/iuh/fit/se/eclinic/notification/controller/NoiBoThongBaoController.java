package iuh.fit.se.eclinic.notification.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.KhoaNoiBo;
import iuh.fit.se.eclinic.notification.dto.request.TaoThongBaoRequest;
import iuh.fit.se.eclinic.notification.dto.response.KetQuaNhanThongBaoResponse;
import iuh.fit.se.eclinic.notification.service.ThongBaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * API nội bộ: service khác (booking-service) gửi thông báo cần tạo. Không qua gateway (gateway chỉ chuyển tiếp
 * {@code /api/notification/**}), không có JWT của người dùng: người gọi chứng minh bằng khoá nội bộ ({@link KhoaNoiBo}).
 */
@Hidden
@RestController
@RequestMapping("/noi-bo/thong-bao")
@RequiredArgsConstructor
public class NoiBoThongBaoController {

    private final ThongBaoService thongBaoService;
    private final KhoaNoiBo khoaNoiBo;

    @PostMapping
    public PhanHoiApi<KetQuaNhanThongBaoResponse> nhan(
            @RequestHeader(name = KhoaNoiBo.TEN_HEADER, required = false) String khoa,
            @RequestBody @NotEmpty @Size(max = 100) List<@Valid TaoThongBaoRequest> danhSach) {
        khoaNoiBo.kiemTra(khoa);
        return PhanHoiApi.ok(thongBaoService.nhan(danhSach));
    }

}
