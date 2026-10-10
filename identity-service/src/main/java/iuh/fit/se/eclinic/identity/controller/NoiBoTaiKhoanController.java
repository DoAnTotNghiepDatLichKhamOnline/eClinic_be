package iuh.fit.se.eclinic.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.security.KhoaNoiBo;
import iuh.fit.se.eclinic.identity.dto.request.SuaThongTinTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.request.TaoTaiKhoanBacSiRequest;
import iuh.fit.se.eclinic.identity.dto.request.VoHieuHoaNoiBoRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanNoiBoResponse;
import iuh.fit.se.eclinic.identity.service.TaiKhoanNoiBoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API nội bộ cho catalog-service quản lý tài khoản của bác sĩ (phase 6). Không đi qua gateway, không cần JWT: mọi method
 * kiểm tra khoá nội bộ ({@link KhoaNoiBo}).
 */
@Hidden
@RestController
@RequestMapping("/noi-bo/tai-khoan")
@RequiredArgsConstructor
public class NoiBoTaiKhoanController {

    private final TaiKhoanNoiBoService taiKhoanNoiBoService;
    private final KhoaNoiBo khoaNoiBo;

    @PostMapping("/bac-si")
    @ResponseStatus(HttpStatus.CREATED)
    public PhanHoiApi<TaiKhoanNoiBoResponse> taoTaiKhoanBacSi(
            @RequestHeader(name = KhoaNoiBo.TEN_HEADER, required = false) String khoa,
            @Valid @RequestBody TaoTaiKhoanBacSiRequest request) {
        khoaNoiBo.kiemTra(khoa);
        return PhanHoiApi.ok(taiKhoanNoiBoService.taoTaiKhoanBacSi(request));
    }

    @DeleteMapping("/bac-si/{id}")
    public PhanHoiApi<Void> xoaTaiKhoanBacSi(@RequestHeader(name = KhoaNoiBo.TEN_HEADER, required = false) String khoa,
            @PathVariable Long id) {
        khoaNoiBo.kiemTra(khoa);
        taiKhoanNoiBoService.xoaTaiKhoanBacSi(id);
        return PhanHoiApi.ok(null);
    }

    @PutMapping("/{id}/thong-tin")
    public PhanHoiApi<TaiKhoanNoiBoResponse> suaThongTin(
            @RequestHeader(name = KhoaNoiBo.TEN_HEADER, required = false) String khoa, @PathVariable Long id,
            @Valid @RequestBody SuaThongTinTaiKhoanRequest request) {
        khoaNoiBo.kiemTra(khoa);
        return PhanHoiApi.ok(taiKhoanNoiBoService.suaThongTin(id, request));
    }

    @PostMapping("/{id}/vo-hieu-hoa")
    public PhanHoiApi<TaiKhoanNoiBoResponse> voHieuHoa(
            @RequestHeader(name = KhoaNoiBo.TEN_HEADER, required = false) String khoa, @PathVariable Long id,
            @Valid @RequestBody VoHieuHoaNoiBoRequest request) {
        khoaNoiBo.kiemTra(khoa);
        return PhanHoiApi.ok(taiKhoanNoiBoService.voHieuHoa(id, request.lyDo()));
    }

    @PostMapping("/{id}/kich-hoat-lai")
    public PhanHoiApi<TaiKhoanNoiBoResponse> kichHoatLai(
            @RequestHeader(name = KhoaNoiBo.TEN_HEADER, required = false) String khoa, @PathVariable Long id) {
        khoaNoiBo.kiemTra(khoa);
        return PhanHoiApi.ok(taiKhoanNoiBoService.kichHoatLai(id));
    }

}
