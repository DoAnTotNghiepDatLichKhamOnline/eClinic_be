package iuh.fit.se.eclinic.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lời gọi giữa các service, prefix {@code app.noi-bo} (application-common.yml).
 *
 * @param khoa khoá chung gửi trong header {@link KhoaNoiBo#TEN_HEADER} khi service này gọi API {@code /noi-bo/**} của
 *             service khác; phải giống nhau ở mọi service
 */
@ConfigurationProperties("app.noi-bo")
public record NoiBoProperties(String khoa) {
}
