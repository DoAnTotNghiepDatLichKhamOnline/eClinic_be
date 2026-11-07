package iuh.fit.se.eclinic.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;
import iuh.fit.se.eclinic.identity.controller.AnhDaiDienController;
import iuh.fit.se.eclinic.identity.service.AnhDaiDienService;

/** Tầng web của /api/users/me/avatar: phải đăng nhập, đọc phần 'anh' của multipart, lỗi đúng mã. */
@WebMvcTest(AnhDaiDienController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class AnhDaiDienControllerTest {

    private static final String URL = "/api/users/me/avatar";
    private static final String URL_ANH = "https://res.cloudinary.com/demo/image/upload/v1/eclinic/avatar/12.jpg";
    private static final byte[] NOI_DUNG = "ÿØÿanh".getBytes(StandardCharsets.ISO_8859_1);

    @Autowired MockMvc mockMvc;
    @MockitoBean AnhDaiDienService anhDaiDienService;

    @Test
    void tepQuaLonTraVe413() throws Exception {
        // MockMvc không áp giới hạn multipart: giả lập đúng exception mà Spring ném khi vượt
        when(anhDaiDienService.taiLen(anyLong(), any())).thenThrow(new MaxUploadSizeExceededException(2 * 1024 * 1024));

        mockMvc.perform(multipart(URL).file(tep("anh")).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.thanhCong").value(false))
                .andExpect(jsonPath("$.maLoi").value("TEP_QUA_LON"))
                .andExpect(jsonPath("$.thongDiep").value("Tệp tải lên vượt quá dung lượng cho phép (tối đa 2 MB)"));
    }

    private static MockMultipartFile tep(String tenPhan) {
        return new MockMultipartFile(tenPhan, "anh.jpg", "image/jpeg", NOI_DUNG);
    }

}
