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
 * @param maTraCuu       mã ngắn in trên phiếu khám (vd ECL-20261005-4198)
 * @param bacSi          null ở danh sách của chính bác sĩ
 * @param idHoSoBenhNhan hồ sơ bệnh nhân của lịch hẹn
 * @param benhNhan       dữ liệu đang lưu trong hồ sơ bệnh nhân
 * @param doiChieu       những gì người đặt nhập cho lượt khám này, để đối chiếu với hồ sơ
 * @param lyDoHuy        lý do bác sĩ từ chối (BI_TU_CHOI) hoặc lý do hủy; null ở các trạng thái khác
 * @param canDoiLich     true: ca khám đã bị hủy, lịch hẹn còn hiệu lực và đang chờ bệnh nhân đổi sang khung giờ khác
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
        Long idHoSoBenhNhan,
        BenhNhan benhNhan,
        NguoiGiamHo nguoiGiamHo,
        DoiChieu doiChieu,
        String lyDoHuy,
        boolean canDoiLich) {

    /**
     * @param tuoi         số tuổi tròn vào ngày khám; null nếu hồ sơ chưa có ngày sinh
     * @param soDienThoai SĐT liên hệ của lượt khám; null khi có người giám hộ (SĐT liên hệ khi đó là của người giám hộ)
     *                    hoặc lịch hẹn tạo trước khi có cột này
     */
    public record BenhNhan(String hoTen, LocalDate ngaySinh, Integer tuoi, GioiTinh gioiTinh, String soDienThoai) {
    }

    public record NguoiGiamHo(String hoTen, QuanHeGiamHo quanHe, String soDienThoai) {
    }

    /**
     * @param canDoiChieu       true nếu họ tên / ngày sinh nhập vào khác hồ sơ (hoặc khác người giám hộ đã khai) và
     *                          phòng khám chưa đối chiếu
     * @param hoTenDaNhap       null với lịch hẹn tạo trước khi lưu thông tin đã nhập (các trường còn lại cũng null)
     * @param hoTenGiamHoDaNhap null nếu lượt khám không có người giám hộ
     */
    public record DoiChieu(boolean canDoiChieu, String hoTenDaNhap, LocalDate ngaySinhDaNhap, GioiTinh gioiTinhDaNhap,
            String hoTenGiamHoDaNhap) {
    }
}
