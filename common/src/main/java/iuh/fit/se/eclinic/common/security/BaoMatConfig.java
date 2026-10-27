package iuh.fit.se.eclinic.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/** Bảo mật chung cho mọi service: stateless, xác thực bằng JWT (Bearer), phân quyền bằng @PreAuthorize. */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(BaoMatProperties.class)
@Import({ JwtConfig.class, LoiBaoMatHandler.class })
public class BaoMatConfig {

    private static final String[] DUONG_DAN_LUON_CONG_KHAI = {
            "/actuator/health/**", "/actuator/info",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/error"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, BaoMatProperties baoMatProperties,
            JwtAuthenticationConverter jwtAuthenticationConverter, LoiBaoMatHandler loiBaoMatHandler)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Thêm Referrer-Policy: token đặt lại mật khẩu nằm trên URL, không để lộ qua header Referer
                .headers(h -> h.referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(DUONG_DAN_LUON_CONG_KHAI).permitAll();
                    for (String duongDan : baoMatProperties.duongDanCongKhai()) {
                        String[] phan = duongDan.trim().split("\\s+", 2);
                        if (phan.length == 2) {
                            auth.requestMatchers(HttpMethod.valueOf(phan[0].toUpperCase()), phan[1]).permitAll();
                        } else {
                            auth.requestMatchers(phan[0]).permitAll();
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(loiBaoMatHandler)
                        .accessDeniedHandler(loiBaoMatHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(loiBaoMatHandler)
                        .accessDeniedHandler(loiBaoMatHandler));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
