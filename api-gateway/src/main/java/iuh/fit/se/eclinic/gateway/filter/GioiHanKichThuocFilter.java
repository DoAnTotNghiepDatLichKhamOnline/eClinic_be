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

/**
 * Từ chối request có body quá lớn (theo header Content-Length) ngay tại gateway, không chuyển tiếp tới service.
 * <p>
 * Gateway không tự đọc multipart mà chuyển nguyên body cho service (Spring Cloud Gateway MVC tắt việc đọc multipart),
 * nên nếu không có filter này thì gateway không có giới hạn dung lượng nào. Mỗi service vẫn có giới hạn riêng nhỏ hơn
 * (vd ảnh đại diện 2MB ở identity-service).
 * <p>
 * Request không có Content-Length (chunked) không bị chặn ở đây; service phía sau vẫn tự chặn.
 * <p>
 * Trước khi trả 413, filter ĐỌC BỎ body (tới {@value #HE_SO_DOC_BO} lần giới hạn). Nếu trả lời rồi đóng kết nối khi
 * client còn đang gửi, TCP báo reset và client có thể không nhận được response: thấy rõ nhất với client gửi
 * "Expect: 100-continue" (curl với body > 1MB), khi đó Tomcat không tự đọc bỏ phần còn lại. Body lớn hơn mức đọc bỏ
 * thì chấp nhận rủi ro đó, đổi lại không phải nhận hết 1 upload khổng lồ.
 */
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
