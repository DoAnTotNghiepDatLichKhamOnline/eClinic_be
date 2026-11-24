package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Dữ liệu mẫu để thử / demo, prefix {@code app.du-lieu-mau} (SEED_DATA / SEED_VOLUME / SEED_PASSWORD /
 * SEED_SHIFTS_UNTIL).
 *
 * @param soNgay số ngày tới luôn có ca mẫu (cửa sổ cuộn)
 * @param denNgay ca mẫu có tới ít nhất ngày này; null (biến để trống) = chỉ dùng cửa sổ cuộn
 */
@ConfigurationProperties("app.du-lieu-mau")
public record DuLieuMauProperties(boolean bat, boolean lon, String matKhau, int soNgay,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {

    /** Ngày cuối cùng cần có ca mẫu khi xếp từ {@code tuNgay}: cuối cửa sổ cuộn hoặc {@code denNgay}, ngày nào xa hơn. */
    public LocalDate denNgayXepCa(LocalDate tuNgay) {
        LocalDate cuoiCuaSo = tuNgay.plusDays(soNgay - 1L);
        return denNgay != null && denNgay.isAfter(cuoiCuaSo) ? denNgay : cuoiCuaSo;
    }

}
