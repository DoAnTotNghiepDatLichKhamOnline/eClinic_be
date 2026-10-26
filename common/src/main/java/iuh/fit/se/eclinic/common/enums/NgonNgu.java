package iuh.fit.se.eclinic.common.enums;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Ngôn ngữ của thông điệp trả về và của email / thông báo. Request chọn ngôn ngữ bằng header Accept-Language:
 * "en..." là tiếng Anh, không gửi hoặc ngôn ngữ khác là tiếng Việt. Tài khoản lưu ngôn ngữ ưa dùng ở
 * tai_khoan.ngon_ngu (dùng khi soạn nội dung không gắn với request nào: email, nhắc lịch).
 */
public enum NgonNgu {

    VI(Locale.forLanguageTag("vi")),
    EN(Locale.ENGLISH);

    private final Locale locale;

    NgonNgu(Locale locale) {
        this.locale = locale;
    }

    public Locale locale() {
        return locale;
    }

    /**
     * Chọn chữ của nội dung có 2 ngôn ngữ (danh mục: chuyên khoa, phòng khám, hồ sơ bác sĩ, thuốc): bản tiếng Anh khi
     * đây là tiếng Anh và bản đó đã được nhập, còn lại là bản tiếng Việt.
     */
    public String chon(String tiengViet, String tiengAnh) {
        return this == EN && tiengAnh != null && !tiengAnh.isBlank() ? tiengAnh : tiengViet;
    }

    public static NgonNgu tu(Locale locale) {
        return locale != null && "en".equals(locale.getLanguage()) ? EN : VI;
    }

    /** Ngôn ngữ của request đang xử lý (do Spring MVC đặt); ngoài request (job, luồng async) là tiếng Việt. */
    public static NgonNgu hienTai() {
        // Không dùng LocaleContextHolder.getLocale(): ngoài request nó trả ngôn ngữ của JVM (trong container là tiếng Anh)
        LocaleContext nguCanh = LocaleContextHolder.getLocaleContext();
        return tu(nguCanh == null ? null : nguCanh.getLocale());
    }

    /** Dùng ở filter bảo mật, nơi Spring MVC chưa đặt ngôn ngữ của request. */
    public static NgonNgu cuaYeuCau(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.ACCEPT_LANGUAGE) == null ? VI : tu(request.getLocale());
    }

}
