package iuh.fit.se.eclinic.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** MẪU test gateway: không cần chạy service thật. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayTest {

    private static final String FRONTEND = "http://localhost:5173";

    /** Request mà service giả nhận được. */
    record YeuCauDaNhan(String phuongThuc, String duongDan, String authorization, String body, String contentType,
            byte[] noiDung) {
    }

    private static final AtomicReference<YeuCauDaNhan> yeuCauCuoi = new AtomicReference<>();
    private static volatile int trangThaiTraVe = 200;
    private static volatile String bodyTraVe = "{}";

    private static final HttpServer serviceGia = khoiDongServiceGia();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort int cong;

    private static HttpServer khoiDongServiceGia() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/catalog/", ApiGatewayTest::xuLyCatalog);
            server.createContext("/api/catalog/cham", exchange -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                traLoi(exchange, 200, "{}");
            });
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void xuLyCatalog(HttpExchange exchange) throws IOException {
        byte[] noiDung = exchange.getRequestBody().readAllBytes();
        yeuCauCuoi.set(new YeuCauDaNhan(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("Authorization"), new String(noiDung, StandardCharsets.UTF_8),
                exchange.getRequestHeaders().getFirst("Content-Type"), noiDung));
        traLoi(exchange, trangThaiTraVe, bodyTraVe);
    }

    private static void traLoi(HttpExchange exchange, int trangThai, String body) throws IOException {
        byte[] noiDung = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(trangThai, noiDung.length);
        exchange.getResponseBody().write(noiDung);
        exchange.close();
    }

    @DynamicPropertySource
    static void cauHinh(DynamicPropertyRegistry registry) {
        registry.add("CATALOG_URL", () -> "http://localhost:" + serviceGia.getAddress().getPort());
        registry.add("IDENTITY_URL", () -> "http://localhost:1");
        registry.add("spring.http.clients.read-timeout", () -> "1s");
        registry.add("CORS_ALLOWED_ORIGINS", () -> FRONTEND);
        registry.add("GATEWAY_MAX_REQUEST_SIZE", () -> "1MB");
    }

    @AfterAll
    static void dungServiceGia() {
        serviceGia.stop(0);
    }

    @BeforeEach
    void datLai() {
        yeuCauCuoi.set(null);
        trangThaiTraVe = 200;
        bodyTraVe = "{}";
    }

    @Test
    void chuyenTiepGiuNguyenDuongDanQueryVaAuthorization() throws Exception {
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/api/catalog/chuyen-khoa?tuKhoa=noi&trang=0"))
                .header("Authorization", "Bearer abc.def.ghi").GET());

        assertThat(phanHoi.statusCode()).isEqualTo(200);
        YeuCauDaNhan daNhan = yeuCauCuoi.get();
        assertThat(daNhan.phuongThuc()).isEqualTo("GET");
        assertThat(daNhan.duongDan()).isEqualTo("/api/catalog/chuyen-khoa?tuKhoa=noi&trang=0");
        assertThat(daNhan.authorization()).isEqualTo("Bearer abc.def.ghi");
    }

    @Test
    void requestQuaLonBiGatewayTuChoi413KhongChuyenTiepVaVanCoHeaderCors() throws Exception {
        // Giới hạn trong test là 1MB
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/api/catalog/tai-len"))
                .header("Content-Type", "application/octet-stream").header("Origin", FRONTEND)
                .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[1024 * 1024 + 1])));

        assertThat(phanHoi.statusCode()).isEqualTo(413);
        assertThat(phanHoi.body()).contains("\"thanhCong\":false").contains("\"maLoi\":\"YEU_CAU_QUA_LON\"")
                .contains("tối đa 1 MB");
        assertThat(phanHoi.headers().allValues("Access-Control-Allow-Origin")).containsExactly(FRONTEND);
        assertThat(yeuCauCuoi.get()).isNull();

        // Client gửi "Expect: 100-continue" (curl với body lớn): vẫn phải nhận được 413 chứ không bị ngắt kết nối
        HttpResponse<String> coExpect = gui(HttpRequest.newBuilder(url("/api/catalog/tai-len")).expectContinue(true)
                .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[3 * 1024 * 1024])));
        assertThat(coExpect.statusCode()).isEqualTo(413);
        assertThat(coExpect.body()).contains("\"maLoi\":\"YEU_CAU_QUA_LON\"");
        assertThat(yeuCauCuoi.get()).isNull();

        // Đúng bằng giới hạn thì vẫn qua
        assertThat(gui(HttpRequest.newBuilder(url("/api/catalog/tai-len"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[1024 * 1024]))).statusCode()).isEqualTo(200);
        assertThat(yeuCauCuoi.get().noiDung()).hasSize(1024 * 1024);
    }

    @Test
    void originKhongDuocPhepBiTuChoi() throws Exception {
        HttpResponse<String> phanHoi = gui(preflight("http://evil.example"));

        assertThat(phanHoi.statusCode()).isEqualTo(403);
        assertThat(yeuCauCuoi.get()).isNull();
    }

    @Test
    void requestThuongCoDungMotHeaderCors() throws Exception {
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/api/catalog/chuyen-khoa"))
                .header("Origin", FRONTEND).GET());

        assertThat(phanHoi.statusCode()).isEqualTo(200);
        assertThat(phanHoi.headers().allValues("Access-Control-Allow-Origin")).containsExactly(FRONTEND);
    }

    @Test
    void serviceChuaChayTraVe503() throws Exception {
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/api/identity/dang-nhap")).GET());

        assertThat(phanHoi.statusCode()).isEqualTo(503);
        assertThat(phanHoi.body()).contains("\"thanhCong\":false").contains("\"maLoi\":\"DICH_VU_KHONG_KHA_DUNG\"");
    }

    @Test
    void serviceTraLoiQuaLauTraVe504() throws Exception {
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/api/catalog/cham")).GET());

        assertThat(phanHoi.statusCode()).isEqualTo(504);
        assertThat(phanHoi.body()).contains("\"thanhCong\":false").contains("\"maLoi\":\"DICH_VU_PHAN_HOI_QUA_LAU\"");
    }

    private HttpRequest.Builder preflight(String origin) {
        return HttpRequest.newBuilder(url("/api/catalog/chuyen-khoa"))
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody());
    }

    private HttpResponse<String> gui(HttpRequest.Builder request) throws Exception {
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI url(String duongDan) {
        return URI.create("http://localhost:" + cong + duongDan);
    }

}
