package iuh.fit.se.eclinic.identity.dulieumau;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dữ liệu mẫu để thử / demo, prefix {@code app.du-lieu-mau} trong application.yml
 * (lấy từ biến môi trường SEED_DATA / SEED_PASSWORD). Mặc định tắt.
 *
 * @param bat     true thì tạo dữ liệu mẫu khi khởi động
 * @param matKhau mật khẩu chung của mọi tài khoản mẫu
 * @param soNgay  số ngày có ca làm việc tính từ hôm nay
 */
@ConfigurationProperties("app.du-lieu-mau")
public record DuLieuMauProperties(boolean bat, String matKhau, int soNgay) {
}
