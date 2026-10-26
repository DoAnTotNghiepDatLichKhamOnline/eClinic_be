package iuh.fit.se.eclinic.identity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;
import iuh.fit.se.eclinic.identity.controller.PhienDangNhapController;
import iuh.fit.se.eclinic.identity.dto.response.PhienDangNhapResponse;
import iuh.fit.se.eclinic.identity.service.PhienDangNhapService;

/**
 * Tầng web của /api/users/me/sessions: phải đăng nhập, id tài khoản và mã phiên hiện tại lấy từ JWT. Service là mock.
 */
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

    @Test
    void danhSachLayIdVaMaPhienTuToken() throws Exception {
        when(phienDangNhapService.layDanhSach(12L, PHIEN_A)).thenReturn(List.of(
                new PhienDangNhapResponse(PHIEN_A, "Chrome", LocalDateTime.of(2026, 10, 1, 8, 0),
                        LocalDateTime.of(2026, 10, 1, 9, 30), LocalDateTime.of(2026, 10, 8, 9, 30), true),
                new PhienDangNhapResponse(PHIEN_B, null, LocalDateTime.of(2026, 9, 30, 20, 0),
                        LocalDateTime.of(2026, 9, 30, 20, 0), LocalDateTime.of(2026, 10, 7, 20, 0), false)));

        mockMvc.perform(get(URL).with(NguoiDungGiaLap.benhNhan(12L, PHIEN_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu.length()").value(2))
                .andExpect(jsonPath("$.duLieu[0].id").value(PHIEN_A))
                .andExpect(jsonPath("$.duLieu[0].thietBi").value("Chrome"))
                .andExpect(jsonPath("$.duLieu[0].dangNhapLuc").value("2026-10-01T08:00:00"))
                .andExpect(jsonPath("$.duLieu[0].hoatDongLuc").value("2026-10-01T09:30:00"))
                .andExpect(jsonPath("$.duLieu[0].hetHanLuc").value("2026-10-08T09:30:00"))
                .andExpect(jsonPath("$.duLieu[0].hienTai").value(true))
                .andExpect(jsonPath("$.duLieu[1].id").value(PHIEN_B))
                .andExpect(jsonPath("$.duLieu[1].hienTai").value(false))
                .andExpect(jsonPath("$.duLieu[0].tokenHash").doesNotExist());
    }

    @Test
    void tokenKhongCoMaPhienThiTruyenNull() throws Exception {
        when(phienDangNhapService.layDanhSach(5L, null)).thenReturn(List.of());

        // Bác sĩ, quản trị viên cũng gọi được
        mockMvc.perform(get(URL).with(NguoiDungGiaLap.bacSi(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu.length()").value(0));
        verify(phienDangNhapService).layDanhSach(5L, null);
        mockMvc.perform(get(URL).with(NguoiDungGiaLap.quanTriVien())).andExpect(status().isOk());
        verify(phienDangNhapService).layDanhSach(1L, null);
    }

    @Test
    void dangXuatMotPhienTruyenIdTrenDuongDan() throws Exception {
        mockMvc.perform(delete(URL + "/" + PHIEN_B).with(NguoiDungGiaLap.benhNhan(12L, PHIEN_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đã đăng xuất thiết bị"));
        verify(phienDangNhapService).dangXuat(12L, PHIEN_A, PHIEN_B);
    }

    @Test
    void dangXuatCacPhienKhacTraVeSoPhien() throws Exception {
        when(phienDangNhapService.dangXuatCacPhienKhac(12L, PHIEN_A)).thenReturn(2);

        mockMvc.perform(delete(URL).with(NguoiDungGiaLap.benhNhan(12L, PHIEN_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu").value(2))
                .andExpect(jsonPath("$.thongDiep").value("Đã đăng xuất 2 thiết bị khác"));
        verify(phienDangNhapService).dangXuatCacPhienKhac(12L, PHIEN_A);
    }

}
