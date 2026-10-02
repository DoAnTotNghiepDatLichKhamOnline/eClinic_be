package iuh.fit.se.eclinic.identity.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class DinhDangAnhTest {

    @Test
    void nhanRaBaDinhDangTheoByteDau() {
        assertThat(DinhDangAnh.nhanDien(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10))).contains(DinhDangAnh.JPEG);
        assertThat(DinhDangAnh.nhanDien(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00)))
                .contains(DinhDangAnh.PNG);
        assertThat(DinhDangAnh.nhanDien("RIFF\u0010\u0000\u0000\u0000WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1)))
                .contains(DinhDangAnh.WEBP);
    }

    @Test
    void tuChoiTepKhongPhaiAnhDuocChapNhan() {
        assertThat(DinhDangAnh.nhanDien("GIF89a....".getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
        assertThat(DinhDangAnh.nhanDien("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(DinhDangAnh.nhanDien("Đây là tệp văn bản".getBytes(StandardCharsets.UTF_8))).isEmpty();
        // RIFF nhưng không phải WebP (vd tệp WAV)
        assertThat(DinhDangAnh.nhanDien("RIFF\u0010\u0000\u0000\u0000WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1)))
                .isEmpty();
    }

    @Test
    void tepRongHoacQuaNganKhongPhaiAnh() {
        assertThat(DinhDangAnh.nhanDien(null)).isEmpty();
        assertThat(DinhDangAnh.nhanDien(new byte[0])).isEmpty();
        assertThat(DinhDangAnh.nhanDien(bytes(0xFF, 0xD8))).isEmpty();
        assertThat(DinhDangAnh.nhanDien(bytes(0x89, 0x50, 0x4E))).isEmpty();
        assertThat(DinhDangAnh.nhanDien("RIFF\u0010\u0000\u0000\u0000WEB".getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
    }

    private static byte[] bytes(int... giaTri) {
        byte[] ketQua = new byte[giaTri.length];
        for (int i = 0; i < giaTri.length; i++) {
            ketQua[i] = (byte) giaTri[i];
        }
        return ketQua;
    }

}
