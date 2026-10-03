package iuh.fit.se.eclinic.booking.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Địa chỉ IP của client gọi API. Request đi qua api-gateway nên địa chỉ kết nối tới service là của gateway; gateway ghi
 * địa chỉ client vào cuối header X-Forwarded-For (cần spring.cloud.gateway.server.webmvc.trusted-proxies).
 */
public final class DiaChiIp {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private DiaChiIp() {
    }

    /**
     * Phần tử CUỐI của X-Forwarded-For (do gateway thêm vào; các phần tử trước do client tự gửi nên không tin được).
     * Không có header (gọi thẳng vào service khi chạy local) thì lấy địa chỉ kết nối.
     */
    public static String cua(HttpServletRequest request) {
        String chuoi = request.getHeader(X_FORWARDED_FOR);
        if (chuoi != null) {
            String cuoi = chuoi.substring(chuoi.lastIndexOf(',') + 1).trim();
            if (!cuoi.isEmpty()) {
                return cuoi;
            }
        }
        return request.getRemoteAddr();
    }

}
