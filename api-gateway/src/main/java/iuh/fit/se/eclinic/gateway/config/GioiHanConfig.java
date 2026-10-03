package iuh.fit.se.eclinic.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

import iuh.fit.se.eclinic.gateway.filter.GioiHanKichThuocFilter;

/**
 * Đăng ký filter giới hạn dung lượng request. Chạy SAU CorsFilter (xem CorsConfig) để response 413 vẫn có header CORS,
 * nếu không trình duyệt chỉ báo lỗi CORS chứ không đọc được thông báo.
 */
@Configuration
@EnableConfigurationProperties(GioiHanProperties.class)
public class GioiHanConfig {

    static final int THU_TU_FILTER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    FilterRegistrationBean<GioiHanKichThuocFilter> gioiHanKichThuocFilter(GioiHanProperties gioiHanProperties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver xuLyLoi) {
        FilterRegistrationBean<GioiHanKichThuocFilter> dangKy = new FilterRegistrationBean<>(
                new GioiHanKichThuocFilter(gioiHanProperties.kichThuocRequestToiDa(), xuLyLoi));
        dangKy.setOrder(THU_TU_FILTER);
        return dangKy;
    }

}
