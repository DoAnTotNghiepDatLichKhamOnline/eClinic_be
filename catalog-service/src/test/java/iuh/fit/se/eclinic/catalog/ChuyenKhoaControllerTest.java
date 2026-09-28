package iuh.fit.se.eclinic.catalog;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import iuh.fit.se.eclinic.catalog.controller.ChuyenKhoaController;
import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.catalog.service.ChuyenKhoaService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;

/**
 * MẪU test controller: chỉ dựng tầng web (không DB), service là mock.
 * Kiểm tra URL, phân quyền, validate dữ liệu vào và định dạng JSON trả về.
 * <p>
 * Luôn cần 2 annotation: {@code @Import(BaoMatConfig.class)} (cấu hình bảo mật JWT)
 * và {@code @AutoConfigureJson} (JsonMapper cho LoiBaoMatHandler, @WebMvcTest không tự nạp).
 */
@WebMvcTest(ChuyenKhoaController.class)
@AutoConfigureJson
@Import(BaoMatConfig.class)
class ChuyenKhoaControllerTest {

    private static final String URL = "/api/catalog/chuyen-khoa";
    private static final String BODY_HOP_LE = "{\"tenChuyenKhoa\": \"Nội tiết\", \"moTa\": \"Tiểu đường, tuyến giáp\"}";

    @Autowired MockMvc mockMvc;
    @MockitoBean ChuyenKhoaService chuyenKhoaService;

    private final ChuyenKhoaResponse noiTiet = new ChuyenKhoaResponse(1L, "Nội tiết", "Tiểu đường, tuyến giáp");

    @Test
    void khachXemDanhSachKhongCanDangNhap() throws Exception {
        when(chuyenKhoaService.timKiem(null, 0, 20)).thenReturn(new TrangDuLieu<>(List.of(noiTiet), 0, 20, 1, 1));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu.noiDung[0].tenChuyenKhoa").value("Nội tiết"))
                .andExpect(jsonPath("$.duLieu.trang").value(0))
                .andExpect(jsonPath("$.duLieu.kichThuoc").value(20));
        verify(chuyenKhoaService).timKiem(null, 0, 20);
    }

    @Test
    void kichThuocTrangQuaLonTraVe400() throws Exception {
        mockMvc.perform(get(URL).param("kichThuoc", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[0].truong").value("kichThuoc"));
        verifyNoInteractions(chuyenKhoaService);
    }

    @Test
    void jsonSaiHoacIdKhongPhaiSoTraVe400() throws Exception {
        mockMvc.perform(post(URL).with(NguoiDungGiaLap.quanTriVien())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tenChuyenKhoa\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"));
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"));
        verifyNoInteractions(chuyenKhoaService);
    }

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

    @Test
    void adminThemChuyenKhoa() throws Exception {
        when(chuyenKhoaService.tao(any(ChuyenKhoaRequest.class))).thenReturn(noiTiet);

        mockMvc.perform(post(URL).with(NguoiDungGiaLap.quanTriVien())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duLieu.id").value(1))
                .andExpect(jsonPath("$.thongDiep").value("Đã thêm chuyên khoa"));
    }

    @Test
    void tenTrongTraVe400KemChiTiet() throws Exception {
        mockMvc.perform(post(URL).with(NguoiDungGiaLap.quanTriVien())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tenChuyenKhoa\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("tenChuyenKhoa")));
        verifyNoInteractions(chuyenKhoaService);
    }

    @Test
    void adminSuaVaXoa() throws Exception {
        when(chuyenKhoaService.capNhat(eq(1L), any(ChuyenKhoaRequest.class))).thenReturn(noiTiet);
        doNothing().when(chuyenKhoaService).xoa(1L);

        mockMvc.perform(put(URL + "/1").with(NguoiDungGiaLap.quanTriVien())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu.tenChuyenKhoa").value("Nội tiết"));
        mockMvc.perform(delete(URL + "/1").with(NguoiDungGiaLap.quanTriVien()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đã xoá chuyên khoa"))
                .andExpect(jsonPath("$.duLieu").doesNotExist());
        verify(chuyenKhoaService).xoa(1L);
    }

    @Test
    void loiNghiepVuTuServiceTraVeDungMaLoi() throws Exception {
        when(chuyenKhoaService.tao(any(ChuyenKhoaRequest.class)))
                .thenThrow(new LoiNghiepVu(MaLoi.TEN_CHUYEN_KHOA_DA_TON_TAI));

        mockMvc.perform(post(URL).with(NguoiDungGiaLap.quanTriVien())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_HOP_LE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.maLoi").value("TEN_CHUYEN_KHOA_DA_TON_TAI"));
    }

}
