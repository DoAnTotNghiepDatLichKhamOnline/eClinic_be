package iuh.fit.se.eclinic.booking.dto.response;

import java.util.List;

/**
 * Mọi thứ để điền sẵn form đặt lịch cho bệnh nhân đã đăng nhập, trong 1 lần gọi.
 *
 * @param banThan       hồ sơ bệnh nhân của tài khoản (cho "đặt cho bản thân"); null nếu tài khoản chưa có hồ sơ; hồ sơ
 *                      đang chờ xác minh thì chỉ có trạng thái
 * @param emailTaiKhoan email đăng nhập, gợi ý cho ô email của form
 * @param nguoiThan     người thân đã lưu, người vừa đặt lịch gần nhất đứng trước
 * @param lanDatGanNhat chuyên khoa và bác sĩ của lịch hẹn gần nhất tài khoản đã đặt; null nếu chưa đặt lần nào
 */
public record ThongTinDatLichResponse(
        HoSoCuaToiResponse banThan,
        String emailTaiKhoan,
        List<NguoiThanDaLuuResponse> nguoiThan,
        LanDatGanNhat lanDatGanNhat) {

    public record LanDatGanNhat(Long idChuyenKhoa, String tenChuyenKhoa, Long idBacSi, String hoTenBacSi) {
    }
}
