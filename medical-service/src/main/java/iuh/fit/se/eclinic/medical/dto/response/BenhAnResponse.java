package iuh.fit.se.eclinic.medical.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * Hồ sơ bệnh án của 1 lượt khám, cho bác sĩ đã khám.
 *
 * @param maTraCuu    mã tra cứu ngắn của lịch hẹn
 * @param ngayTao     thời điểm ghi nhận kết quả khám
 * @param phienBan    1 khi ghi nhận, tăng 1 mỗi lần sửa; gửi lại khi sửa
 * @param ngaySuaCuoi lần sửa gần nhất; null = chưa sửa lần nào
 * @param hanSua      sau thời điểm này hồ sơ chỉ xem được
 */
public record BenhAnResponse(
        Long id,
        Long idLichHen,
        String maTraCuu,
        TrangThaiLichHen trangThaiLichHen,
        String chanDoan,
        String ghiChu,
        LocalDate ngayTaiKhamDeXuat,
        List<DonThuoc> donThuoc,
        LocalDateTime ngayTao,
        int phienBan,
        LocalDateTime ngaySuaCuoi,
        LocalDateTime hanSua) {

    /**
     * Thuốc lấy từ danh mục; khi sửa hồ sơ, gửi lại {@code idThuoc} để giữ dòng thuốc.
     *
     * @param hoatChat  null ở thuốc cũ chưa được quản trị viên bổ sung (cả {@code hamLuong}, {@code duongDung})
     * @param donVi     đơn vị của thuốc trong danh mục
     */
    public record DonThuoc(Long idThuoc, String tenThuoc, String hoatChat, String hamLuong, DuongDungThuoc duongDung,
            String donVi, String lieuDung, Integer soLanMoiNgay, Integer soNgayDung, String ghiChuSuDung) {
    }
}
