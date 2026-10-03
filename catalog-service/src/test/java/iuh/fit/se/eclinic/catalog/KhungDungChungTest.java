package iuh.fit.se.eclinic.catalog;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.security.JwtService;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;

/**
 * Kiểm tra khung dùng chung trong common (PhanHoiApi, XuLyLoiHandler, bảo mật JWT, actuator, OpenAPI)
 * qua controller mẫu ApiMauController chỉ có trong test.
 */
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

    @Test
    void quanTriVienGiaLapVaoDuocTrangAdmin() throws Exception {
        mockMvc.perform(get("/api/mau/chi-admin").with(NguoiDungGiaLap.quanTriVien()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu").value("xin chào admin"));
    }

    @Test
    void duLieuKhongHopLeTraVe400KemChiTiet() throws Exception {
        mockMvc.perform(post("/api/mau/kiem-tra").with(NguoiDungGiaLap.benhNhan(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hoTen\": \"\", \"email\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[*].truong").value(containsInAnyOrder("hoTen", "email")));
    }

    @Test
    void loiNghiepVuVaKhongTimThay() throws Exception {
        mockMvc.perform(get("/api/mau/loi-nghiep-vu").with(NguoiDungGiaLap.bacSi(5L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.maLoi").value("TRUNG_LICH_LAM_VIEC"));
        mockMvc.perform(get("/api/mau/khong-tim-thay").with(NguoiDungGiaLap.bacSi(5L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.maLoi").value("KHONG_TIM_THAY"));
    }

    @Test
    void healthVaApiDocsKhongCanDangNhap() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bearerAuth")));
    }

}
