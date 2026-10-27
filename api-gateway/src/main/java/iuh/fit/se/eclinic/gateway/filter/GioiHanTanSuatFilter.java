package iuh.fit.se.eclinic.gateway.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import iuh.fit.se.eclinic.gateway.config.GioiHanProperties;
import iuh.fit.se.eclinic.gateway.exception.QuaNhieuYeuCauException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/** Giới hạn số request của mỗi địa chỉ IP trong 1 phút, đếm trên Redis (dùng chung cho mọi instance gateway). */
@Slf4j
public class GioiHanTanSuatFilter extends OncePerRequestFilter {

    static final Duration CUA_SO = Duration.ofMinutes(1);
    static final Duration BO_QUA_SAU_LOI = Duration.ofSeconds(30);

    private static final RedisScript<Long> TANG_BO_DEM = RedisScript.of("""
            local soLan = redis.call('INCR', KEYS[1])
            if soLan == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return soLan
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final GioiHanProperties gioiHan;
    private final HandlerExceptionResolver xuLyLoi;

    /** Thời điểm (ms) được dùng lại Redis sau lần lỗi gần nhất; 0 = Redis đang bình thường. */
    private volatile long boQuaDen;

    public GioiHanTanSuatFilter(StringRedisTemplate redisTemplate, GioiHanProperties gioiHan,
            HandlerExceptionResolver xuLyLoi) {
        this.redisTemplate = redisTemplate;
        this.gioiHan = gioiHan;
        this.xuLyLoi = xuLyLoi;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (boQuaDen != 0 && System.currentTimeMillis() < boQuaDen) {
            filterChain.doFilter(request, response);
            return;
        }
        String nhom = nhom(request);
        String khoa = "gioi-han:" + nhom + ":" + request.getRemoteAddr();
        long soLan;
        long giayChoLai = 0;
        try {
            Long ketQua = redisTemplate.execute(TANG_BO_DEM, List.of(khoa), String.valueOf(CUA_SO.toMillis()));
            soLan = ketQua == null ? 0 : ketQua;
            if (soLan > gioiHanCua(nhom)) {
                Long ttl = redisTemplate.getExpire(khoa, TimeUnit.SECONDS);
                giayChoLai = Math.max(1, ttl == null ? CUA_SO.toSeconds() : ttl);
            }
            boQuaDen = 0;
        } catch (RuntimeException ex) {
            boQuaDen = System.currentTimeMillis() + BO_QUA_SAU_LOI.toMillis();
            log.warn("Không đếm được request trên Redis, bỏ qua giới hạn tần suất trong {} giây: {}",
                    BO_QUA_SAU_LOI.toSeconds(), ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }
        if (giayChoLai == 0) {
            filterChain.doFilter(request, response);
            return;
        }
        log.debug("Từ chối {} {}: IP {} vượt giới hạn nhóm {}", request.getMethod(), request.getRequestURI(),
                request.getRemoteAddr(), nhom);
        // Để XuLyLoiGatewayHandler ghi body JSON như mọi lỗi khác của gateway
        if (xuLyLoi.resolveException(request, response, null, new QuaNhieuYeuCauException(giayChoLai)) == null) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    private String nhom(HttpServletRequest request) {
        if (HttpMethod.POST.matches(request.getMethod())) {
            String duongDan = request.getRequestURI();
            if (gioiHan.duongDanXacThuc().contains(duongDan)) {
                return "xac-thuc";
            }
            if (duongDan.startsWith("/api/chatbot/")) {
                return "chatbot";
            }
        }
        return "chung";
    }

    private int gioiHanCua(String nhom) {
        return switch (nhom) {
            case "xac-thuc" -> gioiHan.soYeuCauXacThucMoiPhut();
            case "chatbot" -> gioiHan.soYeuCauChatbotMoiPhut();
            default -> gioiHan.soYeuCauMoiPhut();
        };
    }

}
