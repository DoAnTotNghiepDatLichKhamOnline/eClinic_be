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

/**
 * MẪU test gateway: không cần chạy service thật. Một HTTP server giả (có sẵn trong JDK) đóng vai catalog-service,
 * ghi lại request nhận được và trả về status/body mà test yêu cầu. identity-service trỏ vào cổng 1 (không có gì chạy)
 * để giả lập service chưa khởi động.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayTest {

    private static final String FRONTEND = "http://localhost:5173";

    /** Request mà service giả nhận được. */
    record YeuCauDaNhan(String phuongThuc, String duongDan, String authorization, String body) {
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
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        yeuCauCuoi.set(new YeuCauDaNhan(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("Authorization"), body));
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
    void chuyenTiepBodyCuaPost() throws Exception {
        String body = "{\"tenChuyenKhoa\": \"Nội tiết\"}";
        gui(HttpRequest.newBuilder(url("/api/catalog/chuyen-khoa")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)));

        assertThat(yeuCauCuoi.get().phuongThuc()).isEqualTo("POST");
        assertThat(yeuCauCuoi.get().body()).isEqualTo(body);
    }

    @Test
    void loiCuaServiceDuocChuyenNguyenChoFrontend() throws Exception {
        trangThaiTraVe = 409;
        bodyTraVe = "{\"thanhCong\":false,\"maLoi\":\"TEN_CHUYEN_KHOA_DA_TON_TAI\",\"thongDiep\":\"Tên đã tồn tại\"}";

        HttpResponse<byte[]> phanHoi = httpClient.send(
                HttpRequest.newBuilder(url("/api/catalog/chuyen-khoa")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(phanHoi.statusCode()).isEqualTo(409);
        assertThat(phanHoi.body()).isEqualTo(bodyTraVe.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void preflightCorsDoGatewayTraLoiKhongChuyenTiep() throws Exception {
        HttpResponse<String> phanHoi = gui(preflight(FRONTEND));

        assertThat(phanHoi.statusCode()).isEqualTo(200);
        assertThat(phanHoi.headers().firstValue("Access-Control-Allow-Origin")).hasValue(FRONTEND);
        assertThat(phanHoi.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");
        assertThat(yeuCauCuoi.get()).isNull();
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
    void apiAuthCungDiToiIdentityService() throws Exception {
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/api/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString("{}")));

        // identity trỏ vào cổng 1: 503 nghĩa là đã có route (không có route thì 404)
        assertThat(phanHoi.statusCode()).isEqualTo(503);
        assertThat(phanHoi.body()).contains("\"maLoi\":\"DICH_VU_KHONG_KHA_DUNG\"");
    }

    @Test
    void serviceTraLoiQuaLauTraVe504() throws Exception {
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/api/catalog/cham")).GET());

        assertThat(phanHoi.statusCode()).isEqualTo(504);
        assertThat(phanHoi.body()).contains("\"thanhCong\":false").contains("\"maLoi\":\"DICH_VU_PHAN_HOI_QUA_LAU\"");
    }

    @Test
    void duongDanKhongCoRouteTraVe404() throws Exception {
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/api/khong-co-route")).GET());

        assertThat(phanHoi.statusCode()).isEqualTo(404);
        assertThat(phanHoi.body()).contains("\"thanhCong\":false").contains("\"maLoi\":\"KHONG_TIM_THAY\"");
    }

    @Test
    void healthCuaGatewayLaUp() throws Exception {
        HttpResponse<String> phanHoi = gui(HttpRequest.newBuilder(url("/actuator/health")).GET());

        assertThat(phanHoi.statusCode()).isEqualTo(200);
        assertThat(phanHoi.body()).contains("\"status\":\"UP\"");
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
