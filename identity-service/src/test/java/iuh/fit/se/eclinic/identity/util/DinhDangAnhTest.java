package iuh.fit.se.eclinic.identity.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import iuh.fit.se.eclinic.common.util.DinhDangAnh;

class DinhDangAnhTest {

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

}
