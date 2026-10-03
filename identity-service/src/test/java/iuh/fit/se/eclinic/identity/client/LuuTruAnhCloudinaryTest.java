package iuh.fit.se.eclinic.identity.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.CloudinaryProperties;

/**
 * Kho ảnh Cloudinary với server giả (MockRestServiceServer), không gọi Cloudinary thật: kiểm tra request gửi đi
 * (đường dẫn, tham số, chữ ký, phần tệp) và cách đổi lỗi của Cloudinary thành mã lỗi của hệ thống.
 */
class LuuTruAnhCloudinaryTest {

    private static final String SECRET = "bi-mat-test";
    private static final String URL_UPLOAD = "https://api.cloudinary.com/v1_1/demo/image/upload";
    private static final String URL_DESTROY = "https://api.cloudinary.com/v1_1/demo/image/destroy";
    private static final String URL_ANH = "https://res.cloudinary.com/demo/image/upload/v1759300000/eclinic/avatar/12.jpg";
    private static final byte[] ANH = "ÿØÿnoi-dung-anh".getBytes(StandardCharsets.ISO_8859_1);

    private final AtomicReference<MockClientHttpRequest> yeuCau = new AtomicReference<>();

    private MockRestServiceServer server;
    private LuuTruAnhCloudinary luuTru;

    @BeforeEach
    void khoiTao() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        luuTru = new LuuTruAnhCloudinary(cauHinh("demo", "123456", SECRET), builder);
    }

    @Test
    void chuKyKhopViDuTrongTaiLieuCloudinary() {
        SortedMap<String, String> thamSo = new TreeMap<>();
        thamSo.put("timestamp", "1315060510");
        thamSo.put("public_id", "sample_image");
        thamSo.put("eager", "w_400,h_300,c_pad|w_260,h_200,c_crop");

        // https://cloudinary.com/documentation/authentication_signatures
        assertThat(LuuTruAnhCloudinary.ky(thamSo, "abcd")).isEqualTo("bfd09f95f331f558cbd1320e67aa8d488770583e");
    }

    @Test
    void taiLenGuiThamSoChuKyPhanTepVaTraSecureUrl() {
        server.expect(requestTo(URL_UPLOAD)).andExpect(method(HttpMethod.POST)).andExpect(ghiLai())
                .andRespond(withSuccess("{\"public_id\":\"eclinic/avatar/12\",\"secure_url\":\"" + URL_ANH + "\"}",
                        MediaType.APPLICATION_JSON));
        long truoc = Instant.now().getEpochSecond();

        String url = luuTru.taiLen("avatar/12", ANH);

        assertThat(url).isEqualTo(URL_ANH);
        server.verify();
        assertThat(yeuCau.get().getHeaders().getContentType().isCompatibleWith(MediaType.MULTIPART_FORM_DATA)).isTrue();
        String body = new String(yeuCau.get().getBodyAsBytes(), StandardCharsets.ISO_8859_1);
        SortedMap<String, String> daKy = new TreeMap<>();
        for (String ten : new String[] { "allowed_formats", "invalidate", "overwrite", "public_id", "timestamp",
                "transformation" }) {
            daKy.put(ten, phan(body, ten));
        }
        assertThat(daKy).containsEntry("public_id", "eclinic/avatar/12")
                .containsEntry("overwrite", "true")
                .containsEntry("invalidate", "true")
                .containsEntry("allowed_formats", "jpg,png,webp")
                .containsEntry("transformation", "c_limit,h_512,w_512");
        assertThat(Long.parseLong(daKy.get("timestamp"))).isBetween(truoc, truoc + 5);
        assertThat(phan(body, "api_key")).isEqualTo("123456");
        assertThat(phan(body, "signature")).isEqualTo(LuuTruAnhCloudinary.ky(daKy, SECRET));
        assertThat(body).contains("name=\"file\"; filename=\"anh\"")
                .contains(new String(ANH, StandardCharsets.ISO_8859_1))
                .doesNotContain(SECRET);
    }

    @Test
    void cloudinaryTuChoiAnhThiBaoAnhKhongHopLe() {
        server.expect(requestTo(URL_UPLOAD)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"message\":\"Invalid image file\"}}"));

        assertMaLoi(() -> luuTru.taiLen("avatar/12", ANH), MaLoi.ANH_KHONG_HOP_LE);
    }

    @Test
    void loiXacThucLoiMayChuVaMatKetNoiDeuLa503() {
        server.expect(requestTo(URL_UPLOAD)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"message\":\"Invalid Signature\"}}"));
        server.expect(requestTo(URL_UPLOAD)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo(URL_UPLOAD)).andRespond(withException(new IOException("mất kết nối")));
        // Trả 200 nhưng không có secure_url
        server.expect(requestTo(URL_UPLOAD)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        for (int i = 0; i < 4; i++) {
            assertMaLoi(() -> luuTru.taiLen("avatar/12", ANH), MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        }
        server.verify();
    }

    @Test
    void xoaGuiPublicIdCoChuKyVaChapNhanCaKhongTimThay() {
        server.expect(requestTo(URL_DESTROY)).andExpect(method(HttpMethod.POST)).andExpect(ghiLai())
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL_DESTROY))
                .andRespond(withSuccess("{\"result\":\"not found\"}", MediaType.APPLICATION_JSON));

        luuTru.xoa("avatar/12");
        luuTru.xoa("avatar/12");

        server.verify();
        SortedMap<String, String> form = new TreeMap<>();
        for (String cap : yeuCau.get().getBodyAsString().split("&")) {
            String[] tenGiaTri = cap.split("=", 2);
            form.put(tenGiaTri[0], URLDecoder.decode(tenGiaTri[1], StandardCharsets.UTF_8));
        }
        assertThat(form).containsEntry("public_id", "eclinic/avatar/12").containsEntry("invalidate", "true")
                .containsEntry("api_key", "123456");
        SortedMap<String, String> daKy = new TreeMap<>(form);
        daKy.remove("api_key");
        daKy.remove("signature");
        assertThat(daKy).containsOnlyKeys("invalidate", "public_id", "timestamp");
        assertThat(form.get("signature")).isEqualTo(LuuTruAnhCloudinary.ky(daKy, SECRET));
    }

    @Test
    void xoaLoiThiBao503KeCaLoi400() {
        server.expect(requestTo(URL_DESTROY)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"message\":\"Missing public_id\"}}"));
        server.expect(requestTo(URL_DESTROY)).andRespond(withException(new IOException("mất kết nối")));

        assertMaLoi(() -> luuTru.xoa("avatar/12"), MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        assertMaLoi(() -> luuTru.xoa("avatar/12"), MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
    }

    @Test
    void chuaCauHinhThiBao503VaKhongGoiCloudinary() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer khongGoi = MockRestServiceServer.bindTo(builder).build();
        // Thiếu secret: coi như chưa cấu hình
        LuuTruAnhCloudinary chuaCauHinh = new LuuTruAnhCloudinary(cauHinh("demo", "123456", " "), builder);

        assertThat(chuaCauHinh.daCauHinh()).isFalse();
        assertMaLoi(() -> chuaCauHinh.taiLen("avatar/12", ANH), MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        assertMaLoi(() -> chuaCauHinh.xoa("avatar/12"), MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        assertThat(chuaCauHinh.laAnhCuaKho(URL_ANH)).isFalse();
        khongGoi.verify();
        assertThat(luuTru.daCauHinh()).isTrue();
    }

    @Test
    void chiUrlCuaCloudMinhMoiLaAnhCuaKho() {
        assertThat(luuTru.laAnhCuaKho(URL_ANH)).isTrue();
        assertThat(luuTru.laAnhCuaKho("https://res.cloudinary.com/cloud-khac/image/upload/v1/eclinic/avatar/12.jpg"))
                .isFalse();
        assertThat(luuTru.laAnhCuaKho("https://res.cloudinary.com/demo-khac/image/upload/v1/a.jpg")).isFalse();
        assertThat(luuTru.laAnhCuaKho("https://lh3.googleusercontent.com/a/anh")).isFalse();
        assertThat(luuTru.laAnhCuaKho(null)).isFalse();
    }

    @Test
    void toStringCuaCauHinhKhongLoSecret() {
        assertThat(cauHinh("demo", "123456", SECRET).toString()).doesNotContain(SECRET).contains("demo");
    }

    private RequestMatcher ghiLai() {
        return request -> yeuCau.set((MockClientHttpRequest) request);
    }

    /** Giá trị của 1 phần dạng văn bản trong body multipart. */
    private static String phan(String body, String ten) {
        Matcher m = Pattern.compile("name=\"" + ten + "\"\r\n(?:[^\r\n]+\r\n)*\r\n([^\r\n]*)\r\n--").matcher(body);
        assertThat(m.find()).as("phần '%s' trong body multipart", ten).isTrue();
        return m.group(1);
    }

    private static CloudinaryProperties cauHinh(String cloudName, String apiKey, String apiSecret) {
        return new CloudinaryProperties(cloudName, apiKey, apiSecret, "eclinic", "https://api.cloudinary.com",
                Duration.ofSeconds(20));
    }

    private static void assertMaLoi(Runnable hanhDong, MaLoi maLoi) {
        assertThatThrownBy(hanhDong::run)
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

}
