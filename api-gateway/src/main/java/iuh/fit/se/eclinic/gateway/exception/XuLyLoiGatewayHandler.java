package iuh.fit.se.eclinic.gateway.exception;

import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Lỗi phát sinh tại gateway (không phải lỗi do service trả về — lỗi đó được chuyển nguyên cho frontend):
 * service chưa chạy -> 503, service trả lời quá lâu -> 504, đường dẫn không thuộc route nào -> 404,
 * body request quá lớn -> 413.
 * <p>
 * Cố ý KHÔNG có @ExceptionHandler(Exception.class): lỗi khác rơi về trang lỗi 500 mặc định của Spring Boot
 * để lỗi lập trình thật vẫn lộ ra.
 */
@Slf4j
@RestControllerAdvice
public class XuLyLoiGatewayHandler {

    /** Gateway gọi service thất bại ở tầng kết nối (bị bọc trong UncheckedIOException hoặc ResourceAccessException). */
    @ExceptionHandler({ UncheckedIOException.class, ResourceAccessException.class })
    ResponseEntity<PhanHoiLoiGateway> xuLyLoiKetNoi(RuntimeException loi, HttpServletRequest request) {
        HttpStatus trangThai;
        PhanHoiLoiGateway body;
        if (coNguyenNhan(loi, HttpConnectTimeoutException.class) || coNguyenNhan(loi, ConnectException.class)) {
            trangThai = HttpStatus.SERVICE_UNAVAILABLE;
            body = PhanHoiLoiGateway.loi("DICH_VU_KHONG_KHA_DUNG",
                    "Dịch vụ tạm thời không truy cập được, vui lòng thử lại sau");
        } else if (coNguyenNhan(loi, HttpTimeoutException.class) || coNguyenNhan(loi, SocketTimeoutException.class)) {
            trangThai = HttpStatus.GATEWAY_TIMEOUT;
            body = PhanHoiLoiGateway.loi("DICH_VU_PHAN_HOI_QUA_LAU",
                    "Dịch vụ phản hồi quá lâu, vui lòng thử lại sau");
        } else {
            trangThai = HttpStatus.BAD_GATEWAY;
            body = PhanHoiLoiGateway.loi("LOI_KET_NOI_DICH_VU", "Lỗi kết nối tới dịch vụ, vui lòng thử lại sau");
        }
        log.warn("Gateway không chuyển tiếp được {} {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                trangThai.value(), loi.getCause() != null ? loi.getCause().toString() : loi.toString());
        return ResponseEntity.status(trangThai).body(body);
    }

    /** Body request vượt giới hạn của gateway (GioiHanKichThuocFilter gọi tới đây, request chưa được chuyển tiếp). */
    @ExceptionHandler(YeuCauQuaLonException.class)
    ResponseEntity<PhanHoiLoiGateway> xuLyYeuCauQuaLon(YeuCauQuaLonException loi) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(PhanHoiLoiGateway.loi("YEU_CAU_QUA_LON", "Dữ liệu gửi lên vượt quá dung lượng cho phép (tối đa "
                        + loi.getKichThuocToiDa().toMegabytes() + " MB)"));
    }

    /** Đường dẫn không khớp route nào. */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<PhanHoiLoiGateway> xuLyKhongTimThay(NoResourceFoundException loi) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PhanHoiLoiGateway.loi("KHONG_TIM_THAY", "Không tìm thấy đường dẫn yêu cầu"));
    }

    private static boolean coNguyenNhan(Throwable loi, Class<? extends Throwable> kieu) {
        for (Throwable t = loi; t != null; t = t.getCause()) {
            if (kieu.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

}
