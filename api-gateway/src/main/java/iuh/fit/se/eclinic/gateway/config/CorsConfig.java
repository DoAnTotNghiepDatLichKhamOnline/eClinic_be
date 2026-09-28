package iuh.fit.se.eclinic.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS cho frontend React — chỉ cấu hình ở đây, các service phía sau không cấu hình CORS.
 * Filter tự trả lời request preflight (OPTIONS), không chuyển tiếp tới service.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    CorsFilter corsFilter(CorsProperties corsProperties) {
        CorsConfiguration cauHinh = new CorsConfiguration();
        cauHinh.setAllowedOrigins(corsProperties.nguonDuocPhep());
        cauHinh.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cauHinh.setAllowedHeaders(List.of("*"));
        // Cho phép gửi cookie (vd refresh token sau này); yêu cầu origin cụ thể, không dùng "*"
        cauHinh.setAllowCredentials(true);
        cauHinh.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource nguon = new UrlBasedCorsConfigurationSource();
        nguon.registerCorsConfiguration("/**", cauHinh);
        return new CorsFilter(nguon);
    }

}
