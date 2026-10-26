package iuh.fit.se.eclinic.identity;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.BaoMatConfig;
import iuh.fit.se.eclinic.common.testsupport.NguoiDungGiaLap;
import iuh.fit.se.eclinic.identity.controller.QuanLyTaiKhoanController;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatTrangThaiTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.response.ChiTietTaiKhoanResponse;
import iuh.fit.se.eclinic.identity.dto.response.HoSoBacSiResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanQuanTriResponse;
import iuh.fit.se.eclinic.identity.service.QuanLyTaiKhoanService;

/**
 * Tầng web của /api/users (quản trị viên quản lý tài khoản): chỉ QUAN_TRI_VIEN gọi được, id quản trị viên lấy từ JWT,
 * validate tham số và body. Service là mock.
 */
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

    @Test
    void danhSachChuyenBoLocVaPhanTrangChoServiceVaTraVeTrangDuLieu() throws Exception {
        TaiKhoanQuanTriResponse dong = new TaiKhoanQuanTriResponse(5L, "Nguyễn An", "an@example.com", "0912345678", null,
                VaiTro.BENH_NHAN, TrangThaiTaiKhoan.VO_HIEU_HOA, "Vi phạm", LocalDate.of(1990, 5, 20),
                LocalDateTime.of(2026, 10, 1, 8, 0));
        when(quanLyTaiKhoanService.timKiem(1L, "an", VaiTro.BENH_NHAN, TrangThaiTaiKhoan.VO_HIEU_HOA, 2, 5))
                .thenReturn(new TrangDuLieu<>(List.of(dong), 2, 5, 11, 3));

        mockMvc.perform(get(URL).param("tuKhoa", "an").param("vaiTro", "BENH_NHAN").param("trangThai", "VO_HIEU_HOA")
                .param("trang", "2").param("kichThuoc", "5").with(NguoiDungGiaLap.quanTriVien()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.duLieu.trang").value(2))
                .andExpect(jsonPath("$.duLieu.kichThuoc").value(5))
                .andExpect(jsonPath("$.duLieu.tongSoPhanTu").value(11))
                .andExpect(jsonPath("$.duLieu.tongSoTrang").value(3))
                .andExpect(jsonPath("$.duLieu.noiDung.length()").value(1))
                .andExpect(jsonPath("$.duLieu.noiDung[0].id").value(5))
                .andExpect(jsonPath("$.duLieu.noiDung[0].email").value("an@example.com"))
                .andExpect(jsonPath("$.duLieu.noiDung[0].vaiTro").value("BENH_NHAN"))
                .andExpect(jsonPath("$.duLieu.noiDung[0].trangThai").value("VO_HIEU_HOA"))
                .andExpect(jsonPath("$.duLieu.noiDung[0].lyDoVoHieuHoa").value("Vi phạm"))
                .andExpect(jsonPath("$.duLieu.noiDung[0].ngaySinh").value("1990-05-20"))
                .andExpect(jsonPath("$.duLieu.noiDung[0].matKhauHash").doesNotExist())
                .andExpect(jsonPath("$.duLieu.noiDung[0].googleId").doesNotExist());
    }

    @Test
    void danhSachKhongThamSoDungMacDinhTrang0KichThuoc20() throws Exception {
        mockMvc.perform(get(URL).with(NguoiDungGiaLap.quanTriVien())).andExpect(status().isOk());

        verify(quanLyTaiKhoanService).timKiem(1L, null, null, null, 0, 20);
    }

    @Test
    void thamSoDanhSachSaiTraVe400() throws Exception {
        RequestPostProcessor admin = NguoiDungGiaLap.quanTriVien();
        assertDuLieuKhongHopLe(mockMvc.perform(get(URL).param("trang", "-1").with(admin)));
        assertDuLieuKhongHopLe(mockMvc.perform(get(URL).param("kichThuoc", "101").with(admin)));
        assertDuLieuKhongHopLe(mockMvc.perform(get(URL).param("kichThuoc", "0").with(admin)));
        assertDuLieuKhongHopLe(mockMvc.perform(get(URL).param("vaiTro", "GIAM_DOC").with(admin)));
        assertDuLieuKhongHopLe(mockMvc.perform(get(URL).param("trangThai", "DA_XOA").with(admin)));

        verifyNoInteractions(quanLyTaiKhoanService);
    }

    @Test
    void idKhongPhaiSoTraVe400() throws Exception {
        RequestPostProcessor admin = NguoiDungGiaLap.quanTriVien();
        assertDuLieuKhongHopLe(mockMvc.perform(get(URL + "/abc").with(admin)));
        assertDuLieuKhongHopLe(mockMvc.perform(delete(URL + "/abc").with(admin)));
        assertDuLieuKhongHopLe(mockMvc.perform(put(URL + "/abc/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"trangThai\": \"DA_KICH_HOAT\"}").with(admin)));

        verifyNoInteractions(quanLyTaiKhoanService);
    }

    @Test
    void chiTietLayIdQuanTriVienTuTokenVaTraVeHoSoBacSi() throws Exception {
        when(quanLyTaiKhoanService.layChiTiet(1L, 7L)).thenReturn(chiTietBacSi(TrangThaiTaiKhoan.DA_KICH_HOAT, null));

        mockMvc.perform(get(URL + "/7").with(NguoiDungGiaLap.quanTriVien()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duLieu.id").value(7))
                .andExpect(jsonPath("$.duLieu.vaiTro").value("BAC_SI"))
                .andExpect(jsonPath("$.duLieu.coMatKhau").value(true))
                .andExpect(jsonPath("$.duLieu.lienKetGoogle").value(false))
                .andExpect(jsonPath("$.duLieu.bacSi.tenChuyenKhoa").value("Nội tổng quát"))
                .andExpect(jsonPath("$.duLieu.matKhauHash").doesNotExist())
                .andExpect(jsonPath("$.duLieu.googleId").doesNotExist());
    }

    @Test
    void voHieuHoaVaKichHoatLaiLayIdTuTokenVaTraThongDiepTheoTrangThai() throws Exception {
        when(quanLyTaiKhoanService.capNhatTrangThai(eq(1L), eq(7L), any(CapNhatTrangThaiTaiKhoanRequest.class)))
                .thenReturn(chiTietBacSi(TrangThaiTaiKhoan.VO_HIEU_HOA, "Vi phạm quy định"))
                .thenReturn(chiTietBacSi(TrangThaiTaiKhoan.DA_KICH_HOAT, null));

        doiTrangThai(NguoiDungGiaLap.quanTriVien(), 7L, "VO_HIEU_HOA", "Vi phạm quy định")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đã vô hiệu hoá tài khoản"))
                .andExpect(jsonPath("$.duLieu.trangThai").value("VO_HIEU_HOA"))
                .andExpect(jsonPath("$.duLieu.lyDoVoHieuHoa").value("Vi phạm quy định"));
        verify(quanLyTaiKhoanService).capNhatTrangThai(1L, 7L,
                new CapNhatTrangThaiTaiKhoanRequest(TrangThaiTaiKhoan.VO_HIEU_HOA, "Vi phạm quy định"));

        // Kích hoạt lại không cần lý do
        mockMvc.perform(put(URL + "/7/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"trangThai\": \"DA_KICH_HOAT\"}").with(NguoiDungGiaLap.quanTriVien()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thongDiep").value("Đã kích hoạt lại tài khoản"))
                .andExpect(jsonPath("$.duLieu.trangThai").value("DA_KICH_HOAT"));
        verify(quanLyTaiKhoanService).capNhatTrangThai(1L, 7L,
                new CapNhatTrangThaiTaiKhoanRequest(TrangThaiTaiKhoan.DA_KICH_HOAT, null));
    }

    @Test
    void bodyDoiTrangThaiSaiTraVe400() throws Exception {
        RequestPostProcessor admin = NguoiDungGiaLap.quanTriVien();
        // Thiếu trangThai
        mockMvc.perform(put(URL + "/7/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"lyDo\": \"Vi phạm\"}").with(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("trangThai")));
        // Lý do 501 ký tự
        doiTrangThai(admin, 7L, "VO_HIEU_HOA", "a".repeat(501))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.chiTiet[*].truong").value(hasItem("lyDo")));
        // Trạng thái không có trong enum, body không phải JSON
        assertDuLieuKhongHopLe(doiTrangThai(admin, 7L, "DA_XOA", "Vi phạm"));
        assertDuLieuKhongHopLe(mockMvc.perform(put(URL + "/7/status").contentType(MediaType.APPLICATION_JSON)
                .content("khong phai json").with(admin)));

        verifyNoInteractions(quanLyTaiKhoanService);
    }

    @Test
    void lyDoDung500KyTuDuocChapNhan() throws Exception {
        doiTrangThai(NguoiDungGiaLap.quanTriVien(), 7L, "VO_HIEU_HOA", "a".repeat(500)).andExpect(status().isOk());

        verify(quanLyTaiKhoanService).capNhatTrangThai(1L, 7L,
                new CapNhatTrangThaiTaiKhoanRequest(TrangThaiTaiKhoan.VO_HIEU_HOA, "a".repeat(500)));
    }

    @Test
    void loiNghiepVuGiuNguyenMaLoiVaStatus() throws Exception {
        when(quanLyTaiKhoanService.capNhatTrangThai(eq(1L), eq(7L), any(CapNhatTrangThaiTaiKhoanRequest.class)))
                .thenThrow(new LoiNghiepVu(MaLoi.TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE))
                .thenThrow(new LoiNghiepVu(MaLoi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE, "Tài khoản đã bị vô hiệu hoá từ trước"))
                .thenThrow(new LoiNghiepVu(MaLoi.BAC_SI_CON_LICH_HEN))
                .thenThrow(new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Phải nhập lý do khi vô hiệu hoá tài khoản"));
        when(quanLyTaiKhoanService.layChiTiet(1L, 99L)).thenThrow(new LoiKhongTimThay("TaiKhoan", 99L));
        when(quanLyTaiKhoanService.timKiem(eq(1L), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA));
        doThrow(new LoiNghiepVu(MaLoi.TAI_KHOAN_DANG_DUOC_SU_DUNG)).when(quanLyTaiKhoanService).xoa(1L, 7L);
        RequestPostProcessor admin = NguoiDungGiaLap.quanTriVien();

        doiTrangThai(admin, 7L, "VO_HIEU_HOA", "Vi phạm").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.maLoi").value("TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE"));
        doiTrangThai(admin, 7L, "VO_HIEU_HOA", "Vi phạm").andExpect(status().isConflict())
                .andExpect(jsonPath("$.maLoi").value("TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.thongDiep").value("Tài khoản đã bị vô hiệu hoá từ trước"));
        doiTrangThai(admin, 7L, "VO_HIEU_HOA", "Vi phạm").andExpect(status().isConflict())
                .andExpect(jsonPath("$.maLoi").value("BAC_SI_CON_LICH_HEN"));
        doiTrangThai(admin, 7L, "VO_HIEU_HOA", " ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"))
                .andExpect(jsonPath("$.thongDiep").value("Phải nhập lý do khi vô hiệu hoá tài khoản"));
        mockMvc.perform(get(URL + "/99").with(admin)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.maLoi").value("KHONG_TIM_THAY"));
        // Quản trị viên vừa bị vô hiệu hoá nhưng access token còn hạn
        mockMvc.perform(get(URL).with(admin)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.maLoi").value("TAI_KHOAN_BI_VO_HIEU_HOA"));
        mockMvc.perform(delete(URL + "/7").with(admin)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.maLoi").value("TAI_KHOAN_DANG_DUOC_SU_DUNG"));
    }

    @Test
    void xoaLayIdTuTokenVaTraThongDiep() throws Exception {
        mockMvc.perform(delete(URL + "/7").with(NguoiDungGiaLap.quanTriVien()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thanhCong").value(true))
                .andExpect(jsonPath("$.thongDiep").value("Đã xoá tài khoản"));

        verify(quanLyTaiKhoanService).xoa(1L, 7L);
    }

    /** @param nguoiDung null là không đăng nhập */
    private ResultActions doiTrangThai(RequestPostProcessor nguoiDung, Long id, String trangThai, String lyDo)
            throws Exception {
        String body = """
                {"trangThai": "%s", "lyDo": "%s"}
                """.formatted(trangThai, lyDo);
        MockHttpServletRequestBuilder request = put(URL + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content(body);
        return mockMvc.perform(nguoiDung == null ? request : request.with(nguoiDung));
    }

    private static ResultActions assertDuLieuKhongHopLe(ResultActions ketQua) throws Exception {
        return ketQua.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.thanhCong").value(false))
                .andExpect(jsonPath("$.maLoi").value("DU_LIEU_KHONG_HOP_LE"));
    }

    private static ChiTietTaiKhoanResponse chiTietBacSi(TrangThaiTaiKhoan trangThai, String lyDo) {
        return new ChiTietTaiKhoanResponse(7L, "BS. Trần Bình", "bs@example.com", "0987654321", null, VaiTro.BAC_SI,
                trangThai, lyDo, null, LocalDateTime.of(2026, 10, 1, 8, 0), true, false,
                LocalDateTime.of(2026, 10, 1, 9, 0), (TrangThaiLienKet) null,
                new HoSoBacSiResponse(3L, 2L, "Nội tổng quát", "ThS.BS", "GP-001", 10, "Tiểu sử",
                        TrangThaiBacSi.DANG_CONG_TAC));
    }

}
