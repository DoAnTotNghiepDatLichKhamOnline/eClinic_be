package iuh.fit.se.eclinic.common.util;

import jakarta.servlet.http.HttpServletRequest;

/** Địa chỉ IP của client gọi API. */
public final class DiaChiIp {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private DiaChiIp() {
    }

    /** Phần tử CUỐI của X-Forwarded-For (do gateway thêm; phần tử trước do client gửi, không tin được). */
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
