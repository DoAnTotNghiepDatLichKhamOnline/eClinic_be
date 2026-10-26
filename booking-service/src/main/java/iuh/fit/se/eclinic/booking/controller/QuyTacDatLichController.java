package iuh.fit.se.eclinic.booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.booking.dto.response.QuyTacDatLichResponse;
import iuh.fit.se.eclinic.booking.service.CauHinhService;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import lombok.RequiredArgsConstructor;

/** Quy tắc đặt lịch cho trang đặt lịch / phiếu khám. Công khai (app.bao-mat.duong-dan-cong-khai). */
@Tag(name = "Quy tắc đặt lịch", description = "Đặt trước tối đa bao nhiêu ngày, hủy / đổi lịch trước giờ khám bao lâu")
@RestController
@RequestMapping("/api/booking/quy-tac-dat-lich")
@RequiredArgsConstructor
public class QuyTacDatLichController {

    private final CauHinhService cauHinhService;

    @Operation(summary = "Quy tắc bệnh nhân cần biết: đặt trước tối thiểu (phút) / tối đa (ngày), hạn hủy / đổi lịch"
            + " (phút trước giờ khám), số lần đổi lịch, số lịch còn hiệu lực của 1 người khám. Không cần đăng nhập")
    @GetMapping
    public PhanHoiApi<QuyTacDatLichResponse> xem() {
        return PhanHoiApi.ok(cauHinhService.quyTacCongKhai());
    }

}
