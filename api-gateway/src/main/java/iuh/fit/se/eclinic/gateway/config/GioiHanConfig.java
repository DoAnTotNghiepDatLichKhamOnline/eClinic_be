package iuh.fit.se.eclinic.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerExceptionResolver;

import iuh.fit.se.eclinic.gateway.filter.GioiHanKichThuocFilter;
import iuh.fit.se.eclinic.gateway.filter.GioiHanTanSuatFilter;

/** Đăng ký filter giới hạn tần suất (429) và giới hạn dung lượng request (413). */
@Configuration
@EnableConfigurationProperties(GioiHanProperties.class)
public class GioiHanConfig {

    static final int THU_TU_FILTER_TAN_SUAT = Ordered.HIGHEST_PRECEDENCE + 5;
    static final int THU_TU_FILTER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    FilterRegistrationBean<GioiHanTanSuatFilter> gioiHanTanSuatFilter(StringRedisTemplate redisTemplate,
            GioiHanProperties gioiHanProperties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver xuLyLoi) {
        FilterRegistrationBean<GioiHanTanSuatFilter> dangKy = new FilterRegistrationBean<>(
                new GioiHanTanSuatFilter(redisTemplate, gioiHanProperties, xuLyLoi));
        dangKy.setOrder(THU_TU_FILTER_TAN_SUAT);
        return dangKy;
    }

    @Bean
    FilterRegistrationBean<GioiHanKichThuocFilter> gioiHanKichThuocFilter(GioiHanProperties gioiHanProperties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver xuLyLoi) {
        FilterRegistrationBean<GioiHanKichThuocFilter> dangKy = new FilterRegistrationBean<>(
                new GioiHanKichThuocFilter(gioiHanProperties.kichThuocRequestToiDa(), xuLyLoi));
        dangKy.setOrder(THU_TU_FILTER);
        return dangKy;
    }

}
