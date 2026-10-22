package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Giới hạn khách tự xin cấp lại liên kết phiếu khám (PAT-03, API công khai), prefix {@code app.cap-lai-lien-ket}. Đếm
 * trên Redis theo cửa sổ cố định; Redis lỗi thì bỏ qua giới hạn. Nhân viên cấp lại không bị giới hạn.
 *
 * @param soLanToiDaMoiMa số lần gửi tối đa cho 1 mã tra cứu trong 1 cửa sổ, đúng hay sai SĐT đều tính (chặn cả đoán SĐT
 *                        lẫn gửi email dồn dập)
 * @param soLanToiDaMoiIp số lần gửi tối đa từ 1 địa chỉ IP trong 1 cửa sổ
 * @param cuaSo           độ dài cửa sổ đếm
 */
@ConfigurationProperties("app.cap-lai-lien-ket")
public record CapLaiLienKetProperties(
        @DefaultValue("3") int soLanToiDaMoiMa,
        @DefaultValue("10") int soLanToiDaMoiIp,
        @DefaultValue("1h") Duration cuaSo) {
}
