package iuh.fit.se.eclinic.medical.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * Hồ sơ bệnh án của 1 lượt khám, cho bác sĩ đã khám.
 *
 * @param maTraCuu mã tra cứu ngắn của lịch hẹn
 * @param ngayTao  thời điểm ghi nhận kết quả khám
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
        LocalDateTime ngayTao) {

    /** @param donVi đơn vị của thuốc trong danh mục */
    public record DonThuoc(String tenThuoc, String donVi, String lieuDung, Integer soLanMoiNgay, Integer soNgayDung,
            String ghiChuSuDung) {
    }
}
