package iuh.fit.se.eclinic.booking.dto.response;

/** Quy tắc đặt lịch bệnh nhân cần biết: đặt trước tối đa bao lâu, hạn hủy và đổi lịch. */
public record QuyTacDatLichResponse(
        int datTruocToiThieuPhut,
        int soNgayDatTruocToiDa,
        int huyDoiTruocToiThieuPhut,
        int soLanDoiLichToiDa,
        int soLichHieuLucToiDaMoiHoSo) {
}
