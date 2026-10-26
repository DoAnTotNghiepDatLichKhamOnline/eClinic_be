package iuh.fit.se.eclinic.medical.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.medical.dto.response.LichSuSuaBenhAnResponse;
import iuh.fit.se.eclinic.medical.service.LichSuSuaBenhAnService;
import lombok.RequiredArgsConstructor;

/** Quản trị viên xem lịch sử sửa hồ sơ bệnh án (màn hình Patient Management, lịch hẹn đã hoàn thành). */
@Tag(name = "Quản trị hồ sơ bệnh án", description = "Lịch sử sửa hồ sơ bệnh án của 1 lượt khám (quản trị viên)")
@RestController
@RequestMapping("/api/medical/quan-tri/lich-hen")
@PreAuthorize("hasRole('QUAN_TRI_VIEN')")
@RequiredArgsConstructor
public class QuanTriBenhAnController {

    private final LichSuSuaBenhAnService lichSuSuaBenhAnService;

    @Operation(summary = "Lịch sử sửa hồ sơ bệnh án của 1 lượt khám: mỗi lần sửa có bác sĩ, thời điểm, lý do, phần đã đổi và"
            + " nội dung trước / sau, mới nhất trước (rỗng nếu chưa sửa; 404 nếu lịch hẹn chưa có hồ sơ bệnh án)")
    @GetMapping("/{idLichHen}/benh-an/lich-su")
    public PhanHoiApi<LichSuSuaBenhAnResponse> lichSuSua(@PathVariable Long idLichHen) {
        return PhanHoiApi.ok(lichSuSuaBenhAnService.choQuanTri(idLichHen));
    }

}
