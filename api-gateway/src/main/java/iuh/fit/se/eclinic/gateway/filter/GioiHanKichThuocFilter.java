package iuh.fit.se.eclinic.gateway.filter;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import iuh.fit.se.eclinic.gateway.exception.YeuCauQuaLonException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/** Từ chối request có Content-Length quá lớn ngay tại gateway, không chuyển tiếp tới service. */
@Slf4j
public class GioiHanKichThuocFilter extends OncePerRequestFilter {

    static final int HE_SO_DOC_BO = 4;

    private final DataSize kichThuocToiDa;
    private final HandlerExceptionResolver xuLyLoi;

    public GioiHanKichThuocFilter(DataSize kichThuocToiDa, HandlerExceptionResolver xuLyLoi) {
        this.kichThuocToiDa = kichThuocToiDa;
        this.xuLyLoi = xuLyLoi;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long kichThuoc = request.getContentLengthLong();
        if (kichThuoc <= kichThuocToiDa.toBytes()) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Từ chối {} {}: body {} byte vượt giới hạn {} byte", request.getMethod(), request.getRequestURI(),
                kichThuoc, kichThuocToiDa.toBytes());
        if (kichThuoc <= kichThuocToiDa.toBytes() * HE_SO_DOC_BO) {
            docBoBody(request);
        }
        // Để XuLyLoiGatewayHandler ghi body JSON như mọi lỗi khác của gateway
        if (xuLyLoi.resolveException(request, response, null, new YeuCauQuaLonException(kichThuocToiDa)) == null) {
            response.sendError(HttpStatus.CONTENT_TOO_LARGE.value());
        }
    }

    private static void docBoBody(HttpServletRequest request) {
        byte[] dem = new byte[8192];
        try {
            InputStream body = request.getInputStream();
            while (body.read(dem) != -1) {
                // bỏ
            }
        } catch (IOException ex) {
            // Client bỏ dở giữa chừng: vẫn trả 413 như thường
        }
    }

}
