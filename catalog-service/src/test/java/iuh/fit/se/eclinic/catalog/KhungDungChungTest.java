package iuh.fit.se.eclinic.catalog;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.security.JwtService;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;

/** Kiểm tra khung dùng chung trong common qua controller mẫu ApiMauController chỉ có trong test. */
@SpringBootTest(properties = "app.bao-mat.duong-dan-cong-khai=GET /api/mau/cong-khai")
@AutoConfigureMockMvc
@Import(MySqlTestcontainersConfiguration.class)
class KhungDungChungTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    @Test
    void duongDanCongKhaiKhongCanDangNhap() throws Exception {
        mockMvc.perform(get("/api/mau/cong-khai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
                .andExpect(jsonPath("$.maLoi").doesNotExist())
                .andExpect(jsonPath("$.chiTiet").doesNotExist());
    }

    @Test
    void khongCoTokenHoacTokenSaiTraVe401() throws Exception {
        mockMvc.perform(get("/api/mau/can-dang-nhap"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.thanhCong").value(false))
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        mockMvc.perform(get("/api/mau/can-dang-nhap").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
    }

    @Test
    void tokenThatCuaBenhNhan() throws Exception {
        String token = jwtService.taoAccessToken(42L, "benhnhan@eclinic.local", VaiTro.BENH_NHAN);

        mockMvc.perform(get("/api/mau/can-dang-nhap").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu").value(42));
        mockMvc.perform(get("/api/mau/chi-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.maLoi").value("KHONG_CO_QUYEN"));
    }

}
