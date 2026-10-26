package iuh.fit.se.eclinic.identity;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;
import iuh.fit.se.eclinic.identity.controller.DoiEmailController;
import iuh.fit.se.eclinic.identity.dto.request.DoiEmailRequest;
import iuh.fit.se.eclinic.identity.dto.response.YeuCauDoiEmailResponse;
import iuh.fit.se.eclinic.identity.service.DoiEmailService;

/**
 * Tầng web của /api/users/me/change-email: phải đăng nhập, mọi vai trò đều gọi được, id tài khoản lấy từ JWT, validate
 * dữ liệu vào. Service là mock.
 */
@WebMvcTest(DoiEmailController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class DoiEmailControllerTest {

    private static final String URL = "/api/users/me/change-email";

    @Autowired MockMvc mockMvc;
    @MockitoBean DoiEmailService doiEmailService;

    @Test
    void chuaDangNhapTraVe401() throws Exception {
        guiYeuCau(null, "moi@example.com", "matkhau")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(doiEmailService);
    }

    @Test
    void yeuCauLayIdTuTokenVaTraVeEmailMoi() throws Exception {
        when(doiEmailService.yeuCau(eq(12L), any(DoiEmailRequest.class)))
                .thenReturn(new YeuCauDoiEmailResponse("moi@example.com"));

        guiYeuCau(NguoiDungGiaLap.benhNhan(12L), "Moi@Example.com", "matkhau")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu.emailMoi").value("moi@example.com"))
                .andExpect(jsonPath("$.thongDiep").value("Đã gửi liên kết xác nhận tới email mới, vui lòng kiểm tra hộp thư"));
        verify(doiEmailService).yeuCau(12L, new DoiEmailRequest("Moi@Example.com", "matkhau"));
    }

    @Test
    void bacSiVaQuanTriVienCungGoiDuoc() throws Exception {
        guiYeuCau(NguoiDungGiaLap.bacSi(5L), "bs-moi@example.com", "matkhau").andExpect(status().isOk());
        verify(doiEmailService).yeuCau(5L, new DoiEmailRequest("bs-moi@example.com", "matkhau"));
        guiYeuCau(NguoiDungGiaLap.quanTriVien(), "admin-moi@example.com", "matkhau").andExpect(status().isOk());
        verify(doiEmailService).yeuCau(1L, new DoiEmailRequest("admin-moi@example.com", "matkhau"));
    }

    @Test
    void emailMoiSaiDinhDangRongHoacQuaDaiTraVe400() throws Exception {
        RequestPostProcessor nguoiDung = NguoiDungGiaLap.benhNhan(12L);

        assertLoiTruong(guiYeuCau(nguoiDung, "khong-phai-email", "matkhau"), "emailMoi");
        assertLoiTruong(guiYeuCau(nguoiDung, " ", "matkhau"), "emailMoi");
        assertLoiTruong(guiYeuCau(nguoiDung, "a".repeat(250) + "@example.com", "matkhau"), "emailMoi");
        verifyNoInteractions(doiEmailService);
    }

    @Test
    void matKhauHienTaiRongHoacQuaDaiTraVe400() throws Exception {
        RequestPostProcessor nguoiDung = NguoiDungGiaLap.benhNhan(12L);

        assertLoiTruong(guiYeuCau(nguoiDung, "moi@example.com", ""), "matKhauHienTai");
        assertLoiTruong(guiYeuCau(nguoiDung, "moi@example.com", "a".repeat(1001)), "matKhauHienTai");
        verifyNoInteractions(doiEmailService);
    }

    @Test
    void matKhauHienTaiKhongBiKiemTraQuyTacMatKhau() throws Exception {
        // 25 ký tự nhưng 75 byte: với @MatKhauHopLe sẽ là 400; ở đây phải tới được service (rồi ra "sai mật khẩu")
        guiYeuCau(NguoiDungGiaLap.benhNhan(12L), "moi@example.com", "ệ".repeat(25)).andExpect(status().isOk());
        verify(doiEmailService).yeuCau(12L, new DoiEmailRequest("moi@example.com", "ệ".repeat(25)));
    }

    @Test
    void loiNghiepVuGiuNguyenMaLoiVaStatus() throws Exception {
        when(doiEmailService.yeuCau(eq(12L), any(DoiEmailRequest.class)))
                .thenThrow(new LoiNghiepVu(MaLoi.MAT_KHAU_CU_KHONG_DUNG))
                .thenThrow(new LoiNghiepVu(MaLoi.EMAIL_MOI_TRUNG_EMAIL_CU))
                .thenThrow(new LoiNghiepVu(MaLoi.EMAIL_DA_TON_TAI))
                .thenThrow(new LoiNghiepVu(MaLoi.GUI_LAI_QUA_NHANH))
                .thenThrow(new LoiNghiepVu(MaLoi.TAI_KHOAN_CHUA_CO_MAT_KHAU));
        RequestPostProcessor nguoiDung = NguoiDungGiaLap.benhNhan(12L);

        guiYeuCau(nguoiDung, "moi@example.com", "sai").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("MAT_KHAU_CU_KHONG_DUNG"));
        guiYeuCau(nguoiDung, "moi@example.com", "dung").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("EMAIL_MOI_TRUNG_EMAIL_CU"))
                .andExpect(jsonPath("$.thongDiep").value("Email mới phải khác email hiện tại"));
        guiYeuCau(nguoiDung, "moi@example.com", "dung").andExpect(status().isConflict())
                .andExpect(jsonPath("$.maLoi").value("EMAIL_DA_TON_TAI"));
        guiYeuCau(nguoiDung, "moi@example.com", "dung").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.maLoi").value("GUI_LAI_QUA_NHANH"));
        guiYeuCau(nguoiDung, "moi@example.com", "dung").andExpect(status().isConflict())
                .andExpect(jsonPath("$.maLoi").value("TAI_KHOAN_CHUA_CO_MAT_KHAU"));
    }

    @Test
    void xemYeuCauDangChoCoVaKhongCo() throws Exception {
        when(doiEmailService.layYeuCauDangCho(12L)).thenReturn(new YeuCauDoiEmailResponse("moi@example.com"));
        when(doiEmailService.layYeuCauDangCho(13L)).thenReturn(null);

        mockMvc.perform(get(URL).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu.emailMoi").value("moi@example.com"));
        // Không có yêu cầu: vẫn 200, không có duLieu (trường null không ghi ra JSON)
        mockMvc.perform(get(URL).with(NguoiDungGiaLap.benhNhan(13L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu").doesNotExist());
    }

    @Test
    void huyYeuCauLayIdTuToken() throws Exception {
        mockMvc.perform(delete(URL).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đã huỷ yêu cầu đổi email"));
        verify(doiEmailService).huy(12L);
    }

    /** @param nguoiDung null là không đăng nhập */
    private ResultActions guiYeuCau(RequestPostProcessor nguoiDung, String emailMoi, String matKhau) throws Exception {
        String body = """
                {"emailMoi": "%s", "matKhauHienTai": "%s"}
                """.formatted(emailMoi, matKhau);
        MockHttpServletRequestBuilder request = post(URL).contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(nguoiDung == null ? request : request.with(nguoiDung));
    }

    private ResultActions assertLoiTruong(ResultActions ketQua, String truong) throws Exception {
        return ketQua.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem(truong)));
    }

}
