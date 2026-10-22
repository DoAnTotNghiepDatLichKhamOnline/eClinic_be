package iuh.fit.se.eclinic.booking.service;

import java.time.Duration;

import iuh.fit.se.eclinic.booking.config.DanhGiaProperties;
import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.config.LichLamViecProperties;
import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;

/**
 * Giá trị các quy tắc đặt lịch / xếp ca tại 1 thời điểm (bảng {@code cau_hinh_he_thong}), lấy bằng
 * {@link CauHinhService#hienTai()}. Bất biến: đọc 1 lần ở đầu thao tác rồi dùng cho cả thao tác.
 *
 * @param datTruocToiThieuPhut             lượt khám chỉ đặt được khi còn cách giờ bắt đầu ít nhất chừng này phút
 * @param soNgayDatTruocToiDa              chỉ đặt được lịch trong số ngày tới này, tính từ hôm nay
 * @param soLichHieuLucToiDaMoiHoSo        số lịch hẹn sắp tới còn hiệu lực tối đa của 1 hồ sơ bệnh nhân
 * @param soLichHieuLucToiDaMoiSoDienThoai số lịch hẹn sắp tới còn hiệu lực tối đa đặt bằng cùng 1 SĐT liên hệ
 * @param huyDoiTruocToiThieuPhut          bệnh nhân chỉ hủy / đổi được lịch khi còn cách giờ khám ít nhất chừng này phút
 * @param soLanDoiLichToiDa                số lần đổi lịch tối đa của 1 lần đặt (đếm theo chuỗi lịch cũ)
 * @param guiYeuCauTruocToiThieuGio        bác sĩ chỉ gửi được yêu cầu đổi ca / xin nghỉ khi ca còn cách giờ bắt đầu ít
 *                                         nhất chừng này giờ; giờ mong muốn của yêu cầu đổi ca cũng vậy
 * @param soNgayXepTruocToiDa              quản trị viên chỉ xếp được ca trong số ngày tới này, tính từ hôm nay
 * @param soGioToiDaMoiTuan                tổng giờ các ca còn hoạt động của 1 bác sĩ trong 1 tuần (thứ Hai - Chủ nhật)
 *                                         không vượt số giờ này (SCHED-05)
 * @param soNgayDuocSuaDanhGia             bệnh nhân sửa được đánh giá trong số ngày này kể từ lúc gửi
 */
public record CauHinhHienTai(
        int datTruocToiThieuPhut,
        int soNgayDatTruocToiDa,
        int soLichHieuLucToiDaMoiHoSo,
        int soLichHieuLucToiDaMoiSoDienThoai,
        int huyDoiTruocToiThieuPhut,
        int soLanDoiLichToiDa,
        int guiYeuCauTruocToiThieuGio,
        int soNgayXepTruocToiDa,
        int soGioToiDaMoiTuan,
        int soNgayDuocSuaDanhGia) {

    public static CauHinhHienTai tu(CauHinhHeThong dong) {
        return new CauHinhHienTai(dong.getDatTruocToiThieuPhut(), dong.getSoNgayDatTruocToiDa(),
                dong.getSoLichHieuLucToiDaMoiHoSo(), dong.getSoLichHieuLucToiDaMoiSoDienThoai(),
                dong.getHuyDoiTruocToiThieuPhut(), dong.getSoLanDoiLichToiDa(), dong.getGuiYeuCauTruocToiThieuGio(),
                dong.getSoNgayXepTruocToiDa(), dong.getSoGioToiDaMoiTuan(), dong.getSoNgayDuocSuaDanhGia());
    }

    /** Giá trị của biến môi trường (chỉ dùng để ghi dòng cấu hình đầu tiên). Khoảng thời gian bỏ phần lẻ: 90s = 1 phút. */
    public static CauHinhHienTai tuBienMoiTruong(DatLichProperties datLich, LichLamViecProperties lichLamViec,
            DanhGiaProperties danhGia) {
        return new CauHinhHienTai(Math.toIntExact(datLich.datTruocToiThieu().toMinutes()),
                datLich.soNgayDatTruocToiDa(), datLich.soLichHieuLucToiDaMoiHoSo(),
                datLich.soLichHieuLucToiDaMoiSoDienThoai(), Math.toIntExact(datLich.huyDoiTruocToiThieu().toMinutes()),
                datLich.soLanDoiLichToiDa(), Math.toIntExact(lichLamViec.guiYeuCauTruocToiThieu().toHours()),
                lichLamViec.soNgayXepTruocToiDa(), lichLamViec.soGioToiDaMoiTuan(), danhGia.soNgayDuocSua());
    }

    public Duration datTruocToiThieu() {
        return Duration.ofMinutes(datTruocToiThieuPhut);
    }

    public Duration huyDoiTruocToiThieu() {
        return Duration.ofMinutes(huyDoiTruocToiThieuPhut);
    }

    public Duration guiYeuCauTruocToiThieu() {
        return Duration.ofHours(guiYeuCauTruocToiThieuGio);
    }

    /** {@link #soGioToiDaMoiTuan} tính bằng phút. */
    public long soPhutToiDaMoiTuan() {
        return soGioToiDaMoiTuan * 60L;
    }

}
