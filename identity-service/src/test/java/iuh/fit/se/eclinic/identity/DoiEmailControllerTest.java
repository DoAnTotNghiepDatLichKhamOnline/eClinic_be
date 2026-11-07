package iuh.fit.se.eclinic.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;
import iuh.fit.se.eclinic.identity.controller.DoiEmailController;
import iuh.fit.se.eclinic.identity.dto.request.DoiEmailRequest;
import iuh.fit.se.eclinic.identity.dto.response.YeuCauDoiEmailResponse;
import iuh.fit.se.eclinic.identity.service.DoiEmailService;

/** Tầng web của /api/users/me/change-email: phải đăng nhập, mọi vai trò, validate dữ liệu vào. */
@WebMvcTest(DoiEmailController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class DoiEmailControllerTest {

    private static final String URL = "/api/users/me/change-email";

    @Autowired MockMvc mockMvc;
    @MockitoBean DoiEmailService doiEmailService;

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

    /** Request với người dùng giả lập; {@code nguoiDung} null là không đăng nhập. */
    private ResultActions guiYeuCau(RequestPostProcessor nguoiDung, String emailMoi, String matKhau) throws Exception {
        String body = """
                {"emailMoi": "%s", "matKhauHienTai": "%s"}
                """.formatted(emailMoi, matKhau);
        MockHttpServletRequestBuilder request = post(URL).contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(nguoiDung == null ? request : request.with(nguoiDung));
    }

}
