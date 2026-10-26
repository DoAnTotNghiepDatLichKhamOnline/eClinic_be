package iuh.fit.se.eclinic.booking.dto.response;

import java.util.List;

/**
 * Mọi thứ trang cá nhân của bệnh nhân hiển thị, trong 1 lần gọi.
 *
 * @param hoSo          hồ sơ bệnh nhân của tài khoản; null nếu tài khoản chưa có hồ sơ; hồ sơ đang chờ xác minh thì chỉ
 *                      có trạng thái
 * @param emailTaiKhoan email đăng nhập
 * @param nguoiThan     người thân đã lưu, người vừa đặt lịch gần nhất đứng trước
 * @param lichSapToi    các lịch sắp tới gần nhất của mỗi bên
 * @param lanKhamGanDay các lượt đã khám gần nhất mà tài khoản được xem kết quả (của tôi và của người tôi đặt hộ)
 * @param soLich        tổng số lịch, để hiện số đếm và biết còn lịch ngoài các dòng trả kèm
 */
public record TrangCaNhanResponse(
        HoSoCuaToiResponse hoSo,
        String emailTaiKhoan,
        List<NguoiThanDaLuuResponse> nguoiThan,
        LichSapToi lichSapToi,
        List<LanKhamCuaToiResponse> lanKhamGanDay,
        SoLich soLich) {

    /**
     * @param cuaToi       người khám là chủ tài khoản, bất kể ai đặt
     * @param cuaNguoiKhac lịch đặt cho người thân, hoặc chủ tài khoản là người giám hộ
     */
    public record LichSapToi(List<LichHenCuaToiResponse> cuaToi, List<LichHenCuaToiResponse> cuaNguoiKhac) {
    }

    /**
     * @param lichSu lịch đã khám, đã hủy, bị từ chối hoặc đã qua giờ, của cả hai bên
     * @param daKham số lượt đã khám mà tài khoản được xem kết quả (tổng của lịch sử khám)
     */
    public record SoLich(long sapToiCuaToi, long sapToiCuaNguoiKhac, long lichSu, long daKham) {
    }
}
