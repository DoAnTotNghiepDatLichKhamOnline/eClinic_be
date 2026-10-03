package iuh.fit.se.eclinic.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;
import iuh.fit.se.eclinic.identity.controller.AnhDaiDienController;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.service.AnhDaiDienService;

/**
 * Tầng web của /api/users/me/avatar: phải đăng nhập, id tài khoản lấy từ JWT, nội dung tệp lấy từ phần 'anh' của
 * multipart, các lỗi tải tệp ra đúng mã lỗi. Service là mock.
 */
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
    void chuaDangNhapTraVe401() throws Exception {
        mockMvc.perform(multipart(URL).file(tep("anh")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.maLoi").value("CHUA_DANG_NHAP"));
        mockMvc.perform(delete(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(anhDaiDienService);
    }

    @Test
    void taiLenTruyenIdTuTokenVaNoiDungTep() throws Exception {
        when(anhDaiDienService.taiLen(12L, NOI_DUNG)).thenReturn(hoSo(12L, URL_ANH));

        // Tên tệp và Content-Type khai sai cũng không sao: controller chỉ chuyển nội dung, service tự nhận định dạng
        mockMvc.perform(multipart(URL)
                        .file(new MockMultipartFile("anh", "anh.txt", "text/plain", NOI_DUNG))
                        .with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.thongDiep").value("Đã đổi ảnh đại diện"))
                .andExpect(jsonPath("$.duLieu.id").value(12))
                .andExpect(jsonPath("$.duLieu.anhDaiDien").value(URL_ANH));
        verify(anhDaiDienService).taiLen(12L, NOI_DUNG);
    }

    @Test
    void bacSiVaQuanTriVienCungDoiDuocAnh() throws Exception {
        when(anhDaiDienService.taiLen(anyLong(), any())).thenReturn(hoSo(5L, URL_ANH));

        mockMvc.perform(multipart(URL).file(tep("anh")).with(NguoiDungGiaLap.bacSi(5L))).andExpect(status().isOk());
        verify(anhDaiDienService).taiLen(5L, NOI_DUNG);
        mockMvc.perform(multipart(URL).file(tep("anh")).with(NguoiDungGiaLap.quanTriVien())).andExpect(status().isOk());
        verify(anhDaiDienService).taiLen(1L, NOI_DUNG);
    }

    @Test
    void thieuPhanAnhHoacKhongPhaiMultipartTraVe400() throws Exception {
        mockMvc.perform(multipart(URL).file(tep("tep")).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.thongDiep").value("Thiếu tệp 'anh'"));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"anh\": \"abc\"}")
                        .with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"));
        verifyNoInteractions(anhDaiDienService);
    }

    @Test
    void tepQuaLonTraVe413() throws Exception {
        // MockMvc không áp giới hạn multipart của servlet container; giả lập đúng exception mà Spring ném khi vượt
        when(anhDaiDienService.taiLen(anyLong(), any())).thenThrow(new MaxUploadSizeExceededException(2 * 1024 * 1024));

        mockMvc.perform(multipart(URL).file(tep("anh")).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.thanhCong").value(false))
                .andExpect(jsonPath("$.maLoi").value("TEP_QUA_LON"))
                .andExpect(jsonPath("$.thongDiep").value("Tệp tải lên vượt quá dung lượng cho phép (tối đa 2 MB)"));
    }

    @Test
    void loiCuaServiceRaDungMaLoi() throws Exception {
        when(anhDaiDienService.taiLen(anyLong(), any()))
                .thenThrow(new LoiNghiepVu(MaLoi.ANH_KHONG_HOP_LE))
                .thenThrow(new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG));

        mockMvc.perform(multipart(URL).file(tep("anh")).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("ANH_KHONG_HOP_LE"));
        mockMvc.perform(multipart(URL).file(tep("anh")).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.maLoi").value("LUU_TRU_ANH_KHONG_KHA_DUNG"));
    }

    @Test
    void xoaTraHoSoKhongConAnh() throws Exception {
        when(anhDaiDienService.xoa(12L)).thenReturn(hoSo(12L, null));

        mockMvc.perform(delete(URL).with(NguoiDungGiaLap.benhNhan(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đã bỏ ảnh đại diện"))
                .andExpect(jsonPath("$.duLieu.anhDaiDien").doesNotExist());
        verify(anhDaiDienService).xoa(12L);
    }

    private static MockMultipartFile tep(String tenPhan) {
        return new MockMultipartFile(tenPhan, "anh.jpg", "image/jpeg", NOI_DUNG);
    }

    private static HoSoCaNhanResponse hoSo(Long id, String anhDaiDien) {
        return new HoSoCaNhanResponse(id, "Nguyễn Thị Mai", "mai@example.com", "0912345678", anhDaiDien,
                VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, true, false, LocalDateTime.of(2026, 10, 1, 8, 0),
                null, null);
    }

}
