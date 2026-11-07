package iuh.fit.se.eclinic.identity;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;
import iuh.fit.se.eclinic.identity.controller.HoSoCaNhanController;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;
import iuh.fit.se.eclinic.identity.service.HoSoCaNhanService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;

/** Tầng web của /api/users/me: phải đăng nhập, mọi vai trò, id và mã phiên lấy từ JWT. */
@WebMvcTest(HoSoCaNhanController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class HoSoCaNhanControllerTest {

    private static final String URL = "/api/users/me";
    private static final String URL_DOI_MAT_KHAU = URL + "/change-password";

    @Autowired MockMvc mockMvc;
    @MockitoBean HoSoCaNhanService hoSoCaNhanService;
    @MockitoBean MatKhauService matKhauService;

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

    private ResultActions doiMatKhau(RequestPostProcessor nguoiDung, String body) throws Exception {
        return mockMvc.perform(put(URL_DOI_MAT_KHAU).with(nguoiDung).contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

}
