package iuh.fit.se.eclinic.identity.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import iuh.fit.se.eclinic.common.luutru.CloudinaryProperties;
import iuh.fit.se.eclinic.common.luutru.LuuTruAnhCloudinary;

/** Kho ảnh Cloudinary với server giả (MockRestServiceServer): request gửi đi và cách đổi lỗi. */
class LuuTruAnhCloudinaryTest {

    private static final String SECRET = "bi-mat-test";
    private static final String URL_UPLOAD = "https://api.cloudinary.com/v1_1/demo/image/upload";
    private static final String URL_DESTROY = "https://api.cloudinary.com/v1_1/demo/image/destroy";
    private static final String URL_ANH = "https://res.cloudinary.com/demo/image/upload/v1759300000/eclinic/avatar/12.jpg";
    private static final byte[] ANH = "ÿØÿnoi-dung-anh".getBytes(StandardCharsets.ISO_8859_1);

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
    void toStringCuaCauHinhKhongLoSecret() {
        assertThat(cauHinh("demo", "123456", SECRET).toString()).doesNotContain(SECRET).contains("demo");
    }

    private static CloudinaryProperties cauHinh(String cloudName, String apiKey, String apiSecret) {
        return new CloudinaryProperties(true, cloudName, apiKey, apiSecret, "eclinic", "https://api.cloudinary.com",
                Duration.ofSeconds(20));
    }

}
