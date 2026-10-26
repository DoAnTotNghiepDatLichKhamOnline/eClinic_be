package iuh.fit.se.eclinic.booking.dto.response;

/**
 * Quy tắc đặt lịch bệnh nhân cần biết, để trang đặt lịch / phiếu khám ghi "đặt trước tối đa N ngày", "hủy / đổi trước
 * giờ khám X phút". Công khai, không cần đăng nhập.
 *
 * @param datTruocToiThieuPhut      lượt khám chỉ đặt được khi còn cách giờ bắt đầu ít nhất chừng này phút
 * @param soNgayDatTruocToiDa       chỉ đặt được lịch trong số ngày tới này, tính từ hôm nay
 * @param huyDoiTruocToiThieuPhut   hủy / đổi lịch được khi còn cách giờ khám ít nhất chừng này phút
 * @param soLanDoiLichToiDa         số lần đổi lịch tối đa của 1 lần đặt (0 = không đổi được)
 * @param soLichHieuLucToiDaMoiHoSo 1 người khám có tối đa chừng này lịch hẹn sắp tới còn hiệu lực
 */
public record QuyTacDatLichResponse(
        int datTruocToiThieuPhut,
        int soNgayDatTruocToiDa,
        int huyDoiTruocToiThieuPhut,
        int soLanDoiLichToiDa,
        int soLichHieuLucToiDaMoiHoSo) {
}
