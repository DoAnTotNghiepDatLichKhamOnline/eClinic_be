package iuh.fit.se.eclinic.catalog;

import static org.mockito.Mockito.verifyNoInteractions;
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

import iuh.fit.se.eclinic.catalog.controller.ChuyenKhoaController;
import iuh.fit.se.eclinic.catalog.service.ChuyenKhoaService;
import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;

/** MẪU test controller: chỉ dựng tầng web (không DB), service là mock. */
@WebMvcTest(ChuyenKhoaController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class ChuyenKhoaControllerTest {

    private static final String URL = "/api/catalog/chuyen-khoa";
    private static final String BODY_HOP_LE = "{\"tenChuyenKhoa\": \"Nội tiết\", \"moTa\": \"Tiểu đường, tuyến giáp\"}";

    @Autowired MockMvc mockMvc;
    @MockitoBean ChuyenKhoaService chuyenKhoaService;

    @Test
    void chuaDangNhapHoacKhongPhaiAdminThiKhongDuocThem() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        mockMvc.perform(post(URL).with(NguoiDungGiaLap.bacSi(5L))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.maLoi").value("KHONG_CO_QUYEN"));
        verifyNoInteractions(chuyenKhoaService);
    }

}
