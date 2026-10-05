package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * 1 lịch hẹn trong danh sách của bác sĩ (lịch hẹn trong ngày) hoặc của quản trị viên (lịch hẹn của 1 ca). CHỈ trả cho
 * bác sĩ của lịch hẹn và quản trị viên: số điện thoại KHÔNG che. Không có mã phiếu khám, không có CCCD.
 *
 * @param maTraCuu mã ngắn in trên phiếu khám (vd ECL-20261005-4198)
 * @param bacSi    null ở danh sách của chính bác sĩ
 */
public record LichHenTrongCaResponse(
        Long id,
        String maTraCuu,
        Long idLichLamViec,
        TrangThaiLichHen trangThai,
        Integer soThuTu,
        LocalDateTime gioKhamDuKien,
        LocalDateTime gioBatDauKhung,
        LocalDateTime gioKetThucKhung,
        BacSiTomTatResponse bacSi,
        PhongKhamTomTatResponse phongKham,
        String lyDoKham,
        LocalDateTime ngayDat,
        BenhNhan benhNhan,
        NguoiGiamHo nguoiGiamHo) {

    /**
     * @param tuoi         số tuổi tròn vào ngày khám; null nếu hồ sơ chưa có ngày sinh
     * @param soDienThoai SĐT liên hệ của lượt khám; null khi có người giám hộ (SĐT liên hệ khi đó là của người giám hộ)
     *                    hoặc lịch hẹn tạo trước khi có cột này
     */
    public record BenhNhan(String hoTen, LocalDate ngaySinh, Integer tuoi, GioiTinh gioiTinh, String soDienThoai) {
    }

    public record NguoiGiamHo(String hoTen, QuanHeGiamHo quanHe, String soDienThoai) {
    }
}
