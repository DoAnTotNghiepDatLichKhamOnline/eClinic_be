package iuh.fit.se.eclinic.identity.dulieumau;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dữ liệu mẫu để thử / demo, prefix {@code app.du-lieu-mau} (SEED_DATA / SEED_VOLUME / SEED_PASSWORD). */
@ConfigurationProperties("app.du-lieu-mau")
public record DuLieuMauProperties(boolean bat, boolean lon, String matKhau, int soNgay) {
}
