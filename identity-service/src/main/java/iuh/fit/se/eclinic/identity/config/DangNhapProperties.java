package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Đăng nhập và phiên đăng nhập, prefix {@code app.dang-nhap} trong application.yml.
 *
 * @param soLanSaiToiDa       số lần sai mật khẩu thì bị khoá
 * @param thoiGianKhoa        thời gian khoá, tính từ lần sai thứ {@code soLanSaiToiDa}
 * @param thoiHanRefreshToken thời gian sống của mỗi refresh token (mỗi lần làm mới cấp token mới)
 * @param anHanDungLai        refresh token đã thu hồi được dùng lại trong khoảng này chỉ bị từ chối;
 *                            quá khoảng này thì đăng xuất phiên của token đó
 */
@ConfigurationProperties("app.dang-nhap")
public record DangNhapProperties(
        @DefaultValue("5") int soLanSaiToiDa,
        @DefaultValue("15m") Duration thoiGianKhoa,
        @DefaultValue("7d") Duration thoiHanRefreshToken,
        @DefaultValue("10s") Duration anHanDungLai) {
}
