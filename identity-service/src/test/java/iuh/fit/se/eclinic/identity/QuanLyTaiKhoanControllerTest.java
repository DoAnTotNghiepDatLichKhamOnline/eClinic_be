package iuh.fit.se.eclinic.identity;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import iuh.fit.se.eclinic.identity.controller.QuanLyTaiKhoanController;
import iuh.fit.se.eclinic.identity.service.QuanLyTaiKhoanService;

/** Tầng web của /api/users: chỉ QUAN_TRI_VIEN gọi được, validate tham số và body. */
@WebMvcTest(QuanLyTaiKhoanController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class QuanLyTaiKhoanControllerTest {

    private static final String URL = "/api/users";

    @Autowired MockMvc mockMvc;

    @MockitoBean QuanLyTaiKhoanService quanLyTaiKhoanService;

    @Test
    void chuaDangNhapTraVe401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        mockMvc.perform(get(URL + "/7")).andExpect(status().isUnauthorized());
        doiTrangThai(null, 7L, "VO_HIEU_HOA", "Vi phạm").andExpect(status().isUnauthorized());
        mockMvc.perform(delete(URL + "/7")).andExpect(status().isUnauthorized());

        verifyNoInteractions(quanLyTaiKhoanService);
    }

    @Test
    void benhNhanVaBacSiBiTuChoi403() throws Exception {
        for (RequestPostProcessor nguoiDung : List.of(NguoiDungGiaLap.benhNhan(12L), NguoiDungGiaLap.bacSi(5L))) {
            mockMvc.perform(get(URL).with(nguoiDung)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.maLoi").value("KHONG_CO_QUYEN"));
            mockMvc.perform(get(URL + "/7").with(nguoiDung)).andExpect(status().isForbidden());
            doiTrangThai(nguoiDung, 7L, "VO_HIEU_HOA", "Vi phạm").andExpect(status().isForbidden());
            mockMvc.perform(delete(URL + "/7").with(nguoiDung)).andExpect(status().isForbidden());
        }

        verifyNoInteractions(quanLyTaiKhoanService);
    }

    /** Request với người dùng giả lập; {@code nguoiDung} null là không đăng nhập. */
    private ResultActions doiTrangThai(RequestPostProcessor nguoiDung, Long id, String trangThai, String lyDo)
            throws Exception {
        String body = """
                {"trangThai": "%s", "lyDo": "%s"}
                """.formatted(trangThai, lyDo);
        MockHttpServletRequestBuilder request = put(URL + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content(body);
        return mockMvc.perform(nguoiDung == null ? request : request.with(nguoiDung));
    }

}
