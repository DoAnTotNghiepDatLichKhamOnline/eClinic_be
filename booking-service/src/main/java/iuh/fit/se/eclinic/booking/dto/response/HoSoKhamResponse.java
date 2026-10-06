package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * Hồ sơ khám bác sĩ mở từ 1 lịch hẹn của mình: lịch hẹn, hồ sơ bệnh nhân đầy đủ và các lần khám trước. CHỈ trả cho bác
 * sĩ của lịch hẹn: số CCCD, SĐT KHÔNG che (bác sĩ đối chiếu giấy tờ).
 *
 * @param lichHen      lịch hẹn đang mở, kèm {@code doiChieu} (những gì người đặt nhập)
 * @param ketQua       kết quả khám của lịch hẹn đang mở; null nếu chưa ghi nhận
 * @param lanKhamTruoc các lịch hẹn khác của bệnh nhân (với mọi bác sĩ) có giờ khám trước lịch hẹn này, mới nhất trước
 */
public record HoSoKhamResponse(
        LichHenTrongCaResponse lichHen,
        HoSoBenhNhan hoSoBenhNhan,
        KetQuaKhamResponse ketQua,
        List<LanKham> lanKhamTruoc) {

    /**
     * @param cccd              null với trẻ chưa có CCCD
     * @param tuoi              số tuổi tròn vào ngày khám; null nếu hồ sơ chưa có ngày sinh
     * @param daLienKetTaiKhoan true nếu hồ sơ đã gắn với 1 tài khoản bệnh nhân (chủ tài khoản tự sửa được hồ sơ)
     */
    public record HoSoBenhNhan(Long id, String cccd, String hoTen, LocalDate ngaySinh, Integer tuoi, GioiTinh gioiTinh,
            String soDienThoai, String diaChi, String soBaoHiemYTe, String tienSuBenhLy, boolean daLienKetTaiKhoan) {
    }

    /** @param ketQua null nếu lần khám đó chưa có hồ sơ bệnh án */
    public record LanKham(Long idLichHen, String maTraCuu, LocalDate ngay, LocalDateTime gioKhamDuKien,
            BacSiTomTatResponse bacSi, String tenChuyenKhoa, TrangThaiLichHen trangThai, String lyDoKham,
            KetQuaKhamResponse ketQua) {
    }
}
