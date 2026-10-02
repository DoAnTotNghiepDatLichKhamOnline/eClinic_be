package iuh.fit.se.eclinic.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;
import iuh.fit.se.eclinic.identity.controller.HoSoCaNhanController;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatHoSoRequest;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;
import iuh.fit.se.eclinic.identity.dto.response.HoSoBenhNhanResponse;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.service.HoSoCaNhanService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;

/**
 * Tầng web của /api/users/me (hồ sơ, đổi mật khẩu): phải đăng nhập, mọi vai trò đều gọi được, id tài khoản và mã phiên
 * lấy từ JWT, validate dữ liệu vào. Service là mock.
 */
@WebMvcTest(HoSoCaNhanController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class HoSoCaNhanControllerTest {

    private static final String URL = "/api/users/me";
    private static final String URL_DOI_MAT_KHAU = URL + "/change-password";

    @Autowired MockMvc mockMvc;
    @MockitoBean HoSoCaNhanService hoSoCaNhanService;
    @MockitoBean MatKhauService matKhauService;

    private final HoSoCaNhanResponse hoSo = new HoSoCaNhanResponse(12L, "Nguyễn Thị Mai", "mai@example.com",
            "0912345678", null, VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, true, false,
            LocalDateTime.of(2026, 10, 1, 8, 0), null,
            new HoSoBenhNhanResponse(3L, TrangThaiLienKet.DA_LIEN_KET, "079000000001", "Nguyễn Thị Mai",
                    LocalDate.of(1995, 3, 12), GioiTinh.NU, "0912345678", "12 Nguyễn Văn Bảo", null, null));

    @Test
    void chuaDangNhapTraVe401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"hoTen\": \"Mai\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        verifyNoInteractions(hoSoCaNhanService);
    }

    @Test
    void xemHoSoLayIdTuToken() throws Exception {
        when(hoSoCaNhanService.layHoSo(12L)).thenReturn(hoSo);

        mockMvc.perform(get(URL).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu.id").value(12))
                .andExpect(jsonPath("$.duLieu.email").value("mai@example.com"))
                .andExpect(jsonPath("$.duLieu.vaiTro").value("BENH_NHAN"))
                .andExpect(jsonPath("$.duLieu.coMatKhau").value(true))
                .andExpect(jsonPath("$.duLieu.lienKetGoogle").value(false))
                .andExpect(jsonPath("$.duLieu.hoSoBenhNhan.cccd").value("079000000001"))
                .andExpect(jsonPath("$.duLieu.hoSoBenhNhan.ngaySinh").value("1995-03-12"))
                .andExpect(jsonPath("$.duLieu.bacSi").doesNotExist())
                .andExpect(jsonPath("$.duLieu.matKhauHash").doesNotExist())
                .andExpect(jsonPath("$.duLieu.googleId").doesNotExist());
        verify(hoSoCaNhanService).layHoSo(12L);
    }

    @Test
    void bacSiVaQuanTriVienCungXemDuoc() throws Exception {
        when(hoSoCaNhanService.layHoSo(any())).thenReturn(hoSo);

        mockMvc.perform(get(URL).with(NguoiDungGiaLap.bacSi(5L))).andExpect(status().isOk());
        verify(hoSoCaNhanService).layHoSo(5L);
        mockMvc.perform(get(URL).with(NguoiDungGiaLap.quanTriVien())).andExpect(status().isOk());
        verify(hoSoCaNhanService).layHoSo(1L);
    }

    @Test
    void capNhatGoiServiceVoiIdTuToken() throws Exception {
        when(hoSoCaNhanService.capNhat(eq(12L), any(CapNhatHoSoRequest.class))).thenReturn(hoSo);

        capNhat(NguoiDungGiaLap.benhNhan(12L), "{\"hoTen\": \"Nguyễn Thị Mai\", \"soDienThoai\": \"0912345678\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đã cập nhật hồ sơ cá nhân"))
                .andExpect(jsonPath("$.duLieu.hoTen").value("Nguyễn Thị Mai"));
        verify(hoSoCaNhanService).capNhat(12L, new CapNhatHoSoRequest("Nguyễn Thị Mai", "0912345678"));
    }

    @Test
    void khongGuiSoDienThoaiVanHopLe() throws Exception {
        when(hoSoCaNhanService.capNhat(eq(12L), any(CapNhatHoSoRequest.class))).thenReturn(hoSo);

        capNhat(NguoiDungGiaLap.benhNhan(12L), "{\"hoTen\": \"Nguyễn Thị Mai\"}").andExpect(status().isOk());
        verify(hoSoCaNhanService).capNhat(12L, new CapNhatHoSoRequest("Nguyễn Thị Mai", null));
    }

    @Test
    void duLieuKhongHopLeTraVe400KemTenTruong() throws Exception {
        assertLoiTruong("{\"hoTen\": \"   \", \"soDienThoai\": \"0912345678\"}", "hoTen");
        assertLoiTruong("{\"hoTen\": \"" + "a".repeat(151) + "\"}", "hoTen");
        assertLoiTruong("{\"hoTen\": \"Mai\", \"soDienThoai\": \"12345\"}", "soDienThoai");
        assertLoiTruong("{\"hoTen\": \"Mai\", \"soDienThoai\": \"\"}", "soDienThoai");
        verifyNoInteractions(hoSoCaNhanService);
    }

    @Test
    void doiMatKhauChuaDangNhapTraVe401() throws Exception {
        mockMvc.perform(put(URL_DOI_MAT_KHAU).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matKhauCu\": \"matkhau-cu\", \"matKhauMoi\": \"matkhau-moi\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        verifyNoInteractions(matKhauService);
    }

    @Test
    void doiMatKhauTruyenIdVaMaPhienTuToken() throws Exception {
        doiMatKhau(NguoiDungGiaLap.benhNhan(12L, "phien-a"),
                "{\"matKhauCu\": \"matkhau-cu\", \"matKhauMoi\": \"matkhau-moi\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.thongDiep").value("Đổi mật khẩu thành công, các thiết bị khác đã bị đăng xuất"));
        verify(matKhauService).doiMatKhau(12L, "phien-a", new DoiMatKhauRequest("matkhau-cu", "matkhau-moi"));

        // Token không có claim "phien": service nhận null (không giữ lại phiên nào)
        doiMatKhau(NguoiDungGiaLap.bacSi(5L), "{\"matKhauCu\": \"matkhau-cu\", \"matKhauMoi\": \"matkhau-moi\"}")
                .andExpect(status().isOk());
        verify(matKhauService).doiMatKhau(5L, null, new DoiMatKhauRequest("matkhau-cu", "matkhau-moi"));
    }

    @Test
    void doiMatKhauDuLieuKhongHopLeTraVe400KemTenTruong() throws Exception {
        assertLoiDoiMatKhau("{\"matKhauCu\": \"  \", \"matKhauMoi\": \"matkhau-moi\"}", "matKhauCu");
        assertLoiDoiMatKhau("{\"matKhauMoi\": \"matkhau-moi\"}", "matKhauCu");
        assertLoiDoiMatKhau("{\"matKhauCu\": \"matkhau-cu\", \"matKhauMoi\": \"12345\"}", "matKhauMoi");
        assertLoiDoiMatKhau("{\"matKhauCu\": \"matkhau-cu\"}", "matKhauMoi");
        verifyNoInteractions(matKhauService);
    }

    private ResultActions doiMatKhau(RequestPostProcessor nguoiDung, String body) throws Exception {
        return mockMvc.perform(put(URL_DOI_MAT_KHAU).with(nguoiDung).contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void assertLoiDoiMatKhau(String body, String truong) throws Exception {
        doiMatKhau(NguoiDungGiaLap.benhNhan(12L, "phien-a"), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[0].truong").value(truong));
    }

    private ResultActions capNhat(RequestPostProcessor nguoiDung, String body) throws Exception {
        return mockMvc.perform(put(URL).with(nguoiDung).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertLoiTruong(String body, String truong) throws Exception {
        capNhat(NguoiDungGiaLap.benhNhan(12L), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[0].truong").value(truong));
    }

}
