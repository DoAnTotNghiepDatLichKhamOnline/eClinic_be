package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.AnhDaiDienService;

/** Đổi / bỏ ảnh đại diện với MySQL + Redis thật, và việc dọn ảnh khi quản trị viên xoá tài khoản. */
class AnhDaiDienServiceTest extends KiemThuTichHop {

    private static final String KHO = "https://res.cloudinary.com/demo/image/upload/";
    private static final String ANH_GOOGLE = "https://lh3.googleusercontent.com/a/anh";
    private static final byte[] JPEG = "ÿØÿàanh-jpeg".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] PNG = "\u0089PNG\r\n\u001A\nanh-png".getBytes(StandardCharsets.ISO_8859_1);

    @Autowired AnhDaiDienService anhDaiDienService;
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
    void khoLoiHoacTuChoiAnhThiGiuAnhCu() {
        TaiKhoan taiKhoan = taoTaiKhoan(ANH_GOOGLE);
        when(luuTruAnh.taiLen(anyString(), any()))
                .thenThrow(new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG))
                .thenThrow(new LoiNghiepVu(MaLoi.ANH_KHONG_HOP_LE));

        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), JPEG), MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        assertMaLoi(() -> anhDaiDienService.taiLen(taiKhoan.getId(), JPEG), MaLoi.ANH_KHONG_HOP_LE);

        assertThat(anhTrongDb(taiKhoan)).isEqualTo(ANH_GOOGLE);
    }

    // ---------- Quản trị viên xoá tài khoản ----------

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
