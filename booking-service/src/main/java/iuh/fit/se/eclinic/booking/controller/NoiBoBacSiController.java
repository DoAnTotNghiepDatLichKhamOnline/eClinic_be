package iuh.fit.se.eclinic.booking.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import iuh.fit.se.eclinic.booking.dto.request.HuyCaCuaBacSiRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaCuaBacSiResponse;
import iuh.fit.se.eclinic.booking.service.NgungCongTacService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.KhoaNoiBo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API nội bộ cho catalog-service khi bác sĩ ngừng công tác (phase 6). Không đi qua gateway, không cần JWT: kiểm tra khoá
 * nội bộ ({@link KhoaNoiBo}).
 */
@Hidden
@RestController
@RequestMapping("/noi-bo/bac-si")
@RequiredArgsConstructor
public class NoiBoBacSiController {

    private final NgungCongTacService ngungCongTacService;
    private final KhoaNoiBo khoaNoiBo;

    @PostMapping("/{id}/huy-ca-sap-toi")
    public PhanHoiApi<KetQuaHuyCaCuaBacSiResponse> huyCaSapToi(
            @RequestHeader(name = KhoaNoiBo.TEN_HEADER, required = false) String khoa, @PathVariable Long id,
            @Valid @RequestBody HuyCaCuaBacSiRequest request) {
        khoaNoiBo.kiemTra(khoa);
        return PhanHoiApi.ok(ngungCongTacService.huyCaSapToi(id, request.idTaiKhoanQuanTri(), request.lyDo()));
    }

}
