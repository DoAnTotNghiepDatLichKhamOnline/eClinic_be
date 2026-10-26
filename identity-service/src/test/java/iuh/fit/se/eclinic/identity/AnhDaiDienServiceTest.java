package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.luutru.LuuTruAnh;
import iuh.fit.se.eclinic.identity.config.AnhDaiDienProperties;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.AnhDaiDienService;
import iuh.fit.se.eclinic.identity.service.QuanLyTaiKhoanService;

/**
 * Đổi / bỏ ảnh đại diện với MySQL + Redis thật, và việc dọn ảnh khi quản trị viên xoá tài khoản. Kho ảnh được giả lập
 * (LuuTruAnhCloudinaryTest kiểm tra riêng phần gọi Cloudinary): mỗi test quy định kho trả về gì.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
class AnhDaiDienServiceTest {

    private static final String KHO = "https://res.cloudinary.com/demo/image/upload/";
    private static final String ANH_GOOGLE = "https://lh3.googleusercontent.com/a/anh";
    private static final byte[] JPEG = "ÿØÿàanh-jpeg".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] PNG = "\u0089PNG\r\n\u001A\nanh-png".getBytes(StandardCharsets.ISO_8859_1);

    @MockitoBean LuuTruAnh luuTruAnh;

    @Autowired AnhDaiDienService anhDaiDienService;
    @Autowired QuanLyTaiKhoanService quanLyTaiKhoanService;
    @Autowired AnhDaiDienProperties anhDaiDienProperties;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired StringRedisTemplate redisTemplate;

    @BeforeEach
    void khoAnhDaCauHinh() {
        when(luuTruAnh.daCauHinh()).thenReturn(true);
        when(luuTruAnh.laAnhCuaKho(any())).thenAnswer(goi -> {
            String url = goi.getArgument(0);
            return url != null && url.startsWith(KHO);
        });
    }

    @Test
    void taiLenLuuUrlVaTraHoSoLanSauGhiDeCungMa() {
        TaiKhoan taiKhoan = taoTaiKhoan(null);
        String ma = "avatar/" + taiKhoan.getId();
        when(luuTruAnh.taiLen(ma, JPEG)).thenReturn(KHO + "v1/eclinic/" + ma + ".jpg");
        when(luuTruAnh.taiLen(ma, PNG)).thenReturn(KHO + "v2/eclinic/" + ma + ".png");

        HoSoCaNhanResponse lan1 = anhDaiDienService.taiLen(taiKhoan.getId(), JPEG);

        assertThat(lan1.id()).isEqualTo(taiKhoan.getId());
        assertThat(lan1.anhDaiDien()).isEqualTo(KHO + "v1/eclinic/" + ma + ".jpg");
        assertThat(anhTrongDb(taiKhoan)).isEqualTo(lan1.anhDaiDien());

        HoSoCaNhanResponse lan2 = anhDaiDienService.taiLen(taiKhoan.getId(), PNG);

        assertThat(lan2.anhDaiDien()).isEqualTo(KHO + "v2/eclinic/" + ma + ".png");
        assertThat(anhTrongDb(taiKhoan)).isEqualTo(lan2.anhDaiDien());
        // Ảnh cũ bị ghi đè trong kho (cùng mã), không cần xoá
        verify(luuTruAnh, never()).xoa(anyString());
    }

    @Test
    void taiLenThayAnhGoogleMaKhongDungToiKhoChoAnhCu() {
        TaiKhoan taiKhoan = taoTaiKhoan(ANH_GOOGLE);
        when(luuTruAnh.taiLen(anyString(), any())).thenReturn(KHO + "v1/eclinic/avatar/x.jpg");

        assertThat(anhDaiDienService.taiLen(taiKhoan.getId(), JPEG).anhDaiDien()).startsWith(KHO);
        verify(luuTruAnh, never()).xoa(anyString());
    }

    @Test
    void khongPhaiAnhThiBao400VaKhongGoiKho() {
        TaiKhoan taiKhoan = taoTaiKhoan(ANH_GOOGLE);

        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), "khong phai anh".getBytes(StandardCharsets.UTF_8)),
                MaLoi.ANH_KHONG_HOP_LE);
        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), new byte[0]), MaLoi.ANH_KHONG_HOP_LE);
        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), "GIF89a".getBytes(StandardCharsets.UTF_8)),
                MaLoi.ANH_KHONG_HOP_LE);

        verify(luuTruAnh, never()).taiLen(anyString(), any());
        assertThat(anhTrongDb(taiKhoan)).isEqualTo(ANH_GOOGLE);
        // Ảnh sai định dạng không tính vào số lần tải lên
        assertThat(redisTemplate.hasKey(khoaDem(taiKhoan))).isFalse();
    }

    @Test
    void khoChuaCauHinhThiBao503VaGiuAnhCu() {
        TaiKhoan taiKhoan = taoTaiKhoan(ANH_GOOGLE);
        when(luuTruAnh.daCauHinh()).thenReturn(false);

        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), JPEG), MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);

        verify(luuTruAnh, never()).taiLen(anyString(), any());
        assertThat(anhTrongDb(taiKhoan)).isEqualTo(ANH_GOOGLE);
    }

    @Test
    void khoLoiHoacTuChoiAnhThiGiuAnhCu() {
        TaiKhoan taiKhoan = taoTaiKhoan(ANH_GOOGLE);
        when(luuTruAnh.taiLen(anyString(), any()))
                .thenThrow(new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG))
                .thenThrow(new LoiNghiepVu(MaLoi.ANH_KHONG_HOP_LE));

        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), JPEG), MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), JPEG), MaLoi.ANH_KHONG_HOP_LE);

        assertThat(anhTrongDb(taiKhoan)).isEqualTo(ANH_GOOGLE);
    }

    @Test
    void taiLenQuaSoLanChoPhepThiBao429VaKhongGoiKho() {
        TaiKhoan taiKhoan = taoTaiKhoan(null);
        when(luuTruAnh.taiLen(anyString(), any())).thenReturn(KHO + "v1/eclinic/avatar/x.jpg");
        int toiDa = anhDaiDienProperties.soLanTaiLenToiDa();

        for (int i = 0; i < toiDa; i++) {
            anhDaiDienService.taiLen(taiKhoan.getId(), JPEG);
        }
        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), JPEG), MaLoi.GUI_LAI_QUA_NHANH);

        verify(luuTruAnh, times(toiDa)).taiLen(anyString(), any());
        assertThat(redisTemplate.getExpire(khoaDem(taiKhoan))).isPositive();
        // Tài khoản khác không bị ảnh hưởng
        assertThat(anhDaiDienService.taiLen(taoTaiKhoan(null).getId(), JPEG).anhDaiDien()).startsWith(KHO);
    }

    @Test
    void xoaAnhCuaKhoThiBoUrlVaXoaTrongKho() {
        TaiKhoan taiKhoan = taoTaiKhoan(KHO + "v1/eclinic/avatar/1.jpg");

        HoSoCaNhanResponse ketQua = anhDaiDienService.xoa(taiKhoan.getId());

        assertThat(ketQua.anhDaiDien()).isNull();
        assertThat(anhTrongDb(taiKhoan)).isNull();
        verify(luuTruAnh).xoa("avatar/" + taiKhoan.getId());
    }

    @Test
    void xoaAnhGoogleChiBoUrlKhongGoiKhoKeCaKhiKhoChuaCauHinh() {
        TaiKhoan taiKhoan = taoTaiKhoan(ANH_GOOGLE);
        when(luuTruAnh.daCauHinh()).thenReturn(false);

        assertThat(anhDaiDienService.xoa(taiKhoan.getId()).anhDaiDien()).isNull();

        assertThat(anhTrongDb(taiKhoan)).isNull();
        verify(luuTruAnh, never()).xoa(anyString());
    }

    @Test
    void xoaVanThanhCongKhiKhoLoi() {
        TaiKhoan taiKhoan = taoTaiKhoan(KHO + "v1/eclinic/avatar/1.jpg");
        doThrow(new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG)).when(luuTruAnh).xoa(anyString());

        assertThat(anhDaiDienService.xoa(taiKhoan.getId()).anhDaiDien()).isNull();

        assertThat(anhTrongDb(taiKhoan)).isNull();
    }

    @Test
    void xoaKhiChuaCoAnhKhongLamGi() {
        TaiKhoan taiKhoan = taoTaiKhoan(null);

        assertThat(anhDaiDienService.xoa(taiKhoan.getId()).anhDaiDien()).isNull();

        verify(luuTruAnh, never()).xoa(anyString());
    }

    @Test
    void taiKhoanBiVoHieuHoaHoacKhongTonTaiBiTuChoi() {
        TaiKhoan taiKhoan = taoTaiKhoan(ANH_GOOGLE);
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoanRepository.save(taiKhoan);

        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), JPEG), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> anhDaiDienService.xoa(taiKhoan.getId()), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> anhDaiDienService.taiLen(Long.MAX_VALUE, JPEG), MaLoi.CHUA_DANG_NHAP);
        assertMaLoi(() -> anhDaiDienService.xoa(Long.MAX_VALUE), MaLoi.CHUA_DANG_NHAP);

        assertThat(anhTrongDb(taiKhoan)).isEqualTo(ANH_GOOGLE);
        verify(luuTruAnh, never()).taiLen(anyString(), any());
        verify(luuTruAnh, never()).xoa(anyString());
    }

    // ---------- Quản trị viên xoá tài khoản: ảnh trong kho được dọn nền sau khi commit ----------

    @Test
    void xoaTaiKhoanCoAnhTrongKhoThiXoaAnhTrongKho() {
        TaiKhoan taiKhoan = taoTaiKhoan(KHO + "v1/eclinic/avatar/1.jpg");

        quanLyTaiKhoanService.xoa(idAdmin(), taiKhoan.getId());

        assertThat(taiKhoanRepository.existsById(taiKhoan.getId())).isFalse();
        verify(luuTruAnh, timeout(5000)).xoa("avatar/" + taiKhoan.getId());
    }

    @Test
    void xoaTaiKhoanDungAnhGoogleHoacKhongCoAnhThiKhongGoiKho() {
        TaiKhoan anhGoogle = taoTaiKhoan(ANH_GOOGLE);
        TaiKhoan khongAnh = taoTaiKhoan(null);

        quanLyTaiKhoanService.xoa(idAdmin(), anhGoogle.getId());
        quanLyTaiKhoanService.xoa(idAdmin(), khongAnh.getId());

        assertThat(taiKhoanRepository.existsById(anhGoogle.getId())).isFalse();
        assertThat(taiKhoanRepository.existsById(khongAnh.getId())).isFalse();
        // Việc dọn ảnh chạy nền: đợi tới khi kho được hỏi về ảnh Google rồi mới khẳng định không có lệnh xoá nào
        verify(luuTruAnh, timeout(5000)).laAnhCuaKho(ANH_GOOGLE);
        verify(luuTruAnh, after(300).never()).xoa(anyString());
    }

    @Test
    void khoLoiKhiDonAnhKhongLamHongViecXoaTaiKhoan() {
        TaiKhoan taiKhoan = taoTaiKhoan(KHO + "v1/eclinic/avatar/1.jpg");
        doThrow(new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG)).when(luuTruAnh).xoa(anyString());

        quanLyTaiKhoanService.xoa(idAdmin(), taiKhoan.getId());

        verify(luuTruAnh, timeout(5000)).xoa("avatar/" + taiKhoan.getId());
        assertThat(taiKhoanRepository.existsById(taiKhoan.getId())).isFalse();
    }

    @Test
    void xoaTaiKhoanBiTuChoiThiKhongDungToiKho() {
        TaiKhoan taiKhoan = taoTaiKhoan(KHO + "v1/eclinic/avatar/1.jpg");

        // Tài khoản quản trị viên được bảo vệ; người thực hiện không phải quản trị viên
        assertMaLoi(() -> quanLyTaiKhoanService.xoa(idAdmin(), idAdmin()), MaLoi.TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE);
        assertMaLoi(() -> quanLyTaiKhoanService.xoa(taiKhoan.getId(), taiKhoan.getId()), MaLoi.KHONG_CO_QUYEN);

        assertThat(anhTrongDb(taiKhoan)).isEqualTo(KHO + "v1/eclinic/avatar/1.jpg");
        verify(luuTruAnh, after(300).never()).xoa(anyString());
    }

    /** Quản trị viên mặc định do KhoiTaoAdminRunner tạo (không tạo thêm quản trị viên trong test). */
    private Long idAdmin() {
        return taiKhoanRepository.findByEmail("admin@eclinic.local").orElseThrow().getId();
    }

    private TaiKhoan taoTaiKhoan(String anhDaiDien) {
        TaiKhoan taiKhoan = new TaiKhoan();
        String hauTo = UUID.randomUUID().toString().substring(0, 8);
        taiKhoan.setHoTen("Người dùng " + hauTo);
        taiKhoan.setEmail("anh-" + hauTo + "@example.com");
        taiKhoan.setMatKhauHash("x");
        taiKhoan.setAnhDaiDien(anhDaiDien);
        taiKhoan.setVaiTro(VaiTro.BENH_NHAN);
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
        return taiKhoanRepository.save(taiKhoan);
    }

    private String anhTrongDb(TaiKhoan taiKhoan) {
        return taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getAnhDaiDien();
    }

    private static String khoaDem(TaiKhoan taiKhoan) {
        return "anh-dai-dien:tai-len:" + taiKhoan.getId();
    }

    private static void assertMaLoi(Runnable hanhDong, MaLoi maLoi) {
        assertThatThrownBy(hanhDong::run)
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

}
