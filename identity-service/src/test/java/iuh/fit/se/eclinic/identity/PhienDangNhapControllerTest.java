package iuh.fit.se.eclinic.identity;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.identity.controller.PhienDangNhapController;
import iuh.fit.se.eclinic.identity.service.PhienDangNhapService;

/** Tầng web của /api/users/me/sessions: phải đăng nhập, id tài khoản và mã phiên hiện tại lấy từ JWT. */
@WebMvcTest(PhienDangNhapController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class PhienDangNhapControllerTest {

    private static final String URL = "/api/users/me/sessions";
    private static final String PHIEN_A = "11111111-1111-1111-1111-111111111111";
    private static final String PHIEN_B = "22222222-2222-2222-2222-222222222222";

    @Autowired MockMvc mockMvc;
    @MockitoBean PhienDangNhapService phienDangNhapService;

    @Test
    void chuaDangNhapTraVe401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        mockMvc.perform(delete(URL + "/" + PHIEN_B)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(phienDangNhapService);
    }

}
