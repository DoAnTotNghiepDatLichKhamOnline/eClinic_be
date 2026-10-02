package iuh.fit.se.eclinic.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS cho frontend React — chỉ cấu hình ở đây, các service phía sau không cấu hình CORS.
 * Filter tự trả lời request preflight (OPTIONS), không chuyển tiếp tới service.
 * <p>
 * Chạy trước mọi filter khác của gateway: response do filter sau nó tự trả (vd 413 của GioiHanKichThuocFilter)
 * vẫn phải có header CORS thì trình duyệt mới đọc được.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(CorsProperties corsProperties) {
        CorsConfiguration cauHinh = new CorsConfiguration();
        cauHinh.setAllowedOrigins(corsProperties.nguonDuocPhep());
        cauHinh.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cauHinh.setAllowedHeaders(List.of("*"));
        // Cho phép gửi cookie (refresh token eclinic_rt của /api/auth); yêu cầu origin cụ thể, không dùng "*"
        cauHinh.setAllowCredentials(true);
        cauHinh.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource nguon = new UrlBasedCorsConfigurationSource();
        nguon.registerCorsConfiguration("/**", cauHinh);
        FilterRegistrationBean<CorsFilter> dangKy = new FilterRegistrationBean<>(new CorsFilter(nguon));
        dangKy.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return dangKy;
    }

}
