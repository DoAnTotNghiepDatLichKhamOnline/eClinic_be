package iuh.fit.se.eclinic.booking.service;

import java.time.Duration;

import iuh.fit.se.eclinic.booking.config.DanhGiaProperties;
import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.config.LichLamViecProperties;
import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;

/** Giá trị các quy tắc đặt lịch và xếp ca tại 1 thời điểm (bảng {@code cau_hinh_he_thong}). */
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

    /** Giá trị từ biến môi trường, chỉ dùng để ghi dòng cấu hình đầu tiên (bỏ phần lẻ phút). */
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
