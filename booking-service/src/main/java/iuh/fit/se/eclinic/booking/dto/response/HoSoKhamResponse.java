package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** Hồ sơ khám bác sĩ mở từ 1 lịch hẹn của mình: lịch hẹn, hồ sơ bệnh nhân đầy đủ và các lần khám trước. */
public record HoSoKhamResponse(
        LichHenTrongCaResponse lichHen,
        HoSoBenhNhan hoSoBenhNhan,
        KetQuaKhamResponse ketQua,
        List<LanKham> lanKhamTruoc) {

    public record HoSoBenhNhan(Long id, String cccd, String hoTen, LocalDate ngaySinh, Integer tuoi, GioiTinh gioiTinh,
            String soDienThoai, String diaChi, String soBaoHiemYTe, String tienSuBenhLy, boolean daLienKetTaiKhoan) {
    }

    /** @param ketQua null nếu lần khám đó chưa có hồ sơ bệnh án */
    public record LanKham(Long idLichHen, String maTraCuu, LocalDate ngay, LocalDateTime gioKhamDuKien,
            BacSiTomTatResponse bacSi, String tenChuyenKhoa, TrangThaiLichHen trangThai, String lyDoKham,
            KetQuaKhamResponse ketQua) {
    }
}
