package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDateTime;

/**
 * Một phiên đăng nhập (1 thiết bị) đang còn hiệu lực của người dùng.
 *
 * @param id          mã phiên, không đổi qua các lần làm mới token; dùng để đăng xuất phiên này
 * @param thietBi     User-Agent lúc đăng nhập (có thể null); frontend tự rút gọn thành tên trình duyệt / thiết bị
 * @param dangNhapLuc thời điểm đăng nhập
 * @param hoatDongLuc lần làm mới token gần nhất
 * @param hetHanLuc   không làm mới trước thời điểm này thì phiên tự hết hạn
 * @param hienTai     true nếu là phiên của access token đang gọi API
 */
public record PhienDangNhapResponse(
        String id,
        String thietBi,
        LocalDateTime dangNhapLuc,
        LocalDateTime hoatDongLuc,
        LocalDateTime hetHanLuc,
        boolean hienTai) {
}
