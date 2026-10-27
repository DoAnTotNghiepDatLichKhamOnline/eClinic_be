package iuh.fit.se.eclinic.gateway.exception;

import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Lỗi phát sinh tại gateway: 503 service chưa chạy, 504 quá lâu, 404 không có route, 413, 429. */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class XuLyLoiGatewayHandler {

    private static final Locale TIENG_VIET = Locale.forLanguageTag("vi");

    private final MessageSource messageSource;

    /** Gateway gọi service thất bại ở tầng kết nối (UncheckedIOException / ResourceAccessException). */
    @ExceptionHandler({ UncheckedIOException.class, ResourceAccessException.class })
    ResponseEntity<PhanHoiLoiGateway> xuLyLoiKetNoi(RuntimeException loi, HttpServletRequest request) {
        HttpStatus trangThai;
        PhanHoiLoiGateway body;
        if (coNguyenNhan(loi, HttpConnectTimeoutException.class) || coNguyenNhan(loi, ConnectException.class)) {
            trangThai = HttpStatus.SERVICE_UNAVAILABLE;
            body = loi("DICH_VU_KHONG_KHA_DUNG", request);
        } else if (coNguyenNhan(loi, HttpTimeoutException.class) || coNguyenNhan(loi, SocketTimeoutException.class)) {
            trangThai = HttpStatus.GATEWAY_TIMEOUT;
            body = loi("DICH_VU_PHAN_HOI_QUA_LAU", request);
        } else {
            trangThai = HttpStatus.BAD_GATEWAY;
            body = loi("LOI_KET_NOI_DICH_VU", request);
        }
        log.warn("Gateway không chuyển tiếp được {} {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                trangThai.value(), loi.getCause() != null ? loi.getCause().toString() : loi.toString());
        return ResponseEntity.status(trangThai).body(body);
    }

    /** Body request vượt giới hạn của gateway (gọi từ GioiHanKichThuocFilter). */
    @ExceptionHandler(YeuCauQuaLonException.class)
    ResponseEntity<PhanHoiLoiGateway> xuLyYeuCauQuaLon(YeuCauQuaLonException loi, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(loi("YEU_CAU_QUA_LON", request, String.valueOf(loi.getKichThuocToiDa().toMegabytes())));
    }

    /** Địa chỉ IP vượt giới hạn tần suất (gọi từ GioiHanTanSuatFilter). */
    @ExceptionHandler(QuaNhieuYeuCauException.class)
    ResponseEntity<PhanHoiLoiGateway> xuLyQuaNhieuYeuCau(QuaNhieuYeuCauException loi, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(loi.getGiayChoLai()))
                .body(loi("QUA_NHIEU_YEU_CAU", request, String.valueOf(loi.getGiayChoLai())));
    }

    /** Đường dẫn không khớp route nào. */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<PhanHoiLoiGateway> xuLyKhongTimThay(NoResourceFoundException loi, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(loi("KHONG_TIM_THAY", request));
    }

    /** Request không gửi Accept-Language hoặc gửi ngôn ngữ khác tiếng Anh: tiếng Việt. */
    private PhanHoiLoiGateway loi(String maLoi, HttpServletRequest request, Object... thamSo) {
        boolean tiengAnh = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE) != null
                && "en".equals(request.getLocale().getLanguage());
        return PhanHoiLoiGateway.loi(maLoi, messageSource.getMessage("loi." + maLoi, thamSo, maLoi,
                tiengAnh ? Locale.ENGLISH : TIENG_VIET));
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
