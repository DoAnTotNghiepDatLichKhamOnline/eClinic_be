package iuh.fit.se.eclinic.common.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Ghi Cache-Control cho route có {@link CacheCongKhai}: {@code public, max-age, s-maxage} (s-maxage để CDN của Vercel
 * cache, dùng chung cho mọi người) và {@code Vary: Accept-Language} vì nội dung theo ngôn ngữ của request. Chỉ áp cho GET
 * trả 200; route khác giữ mặc định no-store của Spring Security (Spring Security không ghi đè khi đã có Cache-Control).
 */
@RestControllerAdvice
public class CacheCongKhaiAdvice implements ResponseBodyAdvice<Object> {

    private final long giay;
    private final boolean choCdn;

    public CacheCongKhaiAdvice(@Value("${app.cache-cong-khai.giay:60}") long giay,
            @Value("${app.cache-cong-khai.cdn:true}") boolean choCdn) {
        this.giay = giay;
        this.choCdn = choCdn;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return giay > 0 && returnType.hasMethodAnnotation(CacheCongKhai.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!HttpMethod.GET.equals(request.getMethod())
                || !(request instanceof ServletServerHttpRequest yeuCau)
                || !(response instanceof ServletServerHttpResponse phanHoi)
                || phanHoi.getServletResponse().getStatus() != HttpStatus.OK.value()) {
            return body;
        }
        CacheCongKhai cauHinh = returnType.getMethodAnnotation(CacheCongKhai.class);
        for (String thamSo : cauHinh.boQuaKhiCo()) {
            if (yeuCau.getServletRequest().getParameter(thamSo) != null) {
                return body;
            }
        }
        CacheControl cache = CacheControl.maxAge(Duration.ofSeconds(giay));
        cache = choCdn ? cache.cachePublic().sMaxAge(Duration.ofSeconds(giay)) : cache.cachePrivate();
        response.getHeaders().setCacheControl(cache);
        response.getHeaders().add(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
        return body;
    }

}
