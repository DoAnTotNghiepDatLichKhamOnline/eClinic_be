package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Kết quả 1 lượt khám (đọc từ hồ sơ bệnh án của medical-service), dùng chung cho hồ sơ khám của bác sĩ và lịch sử khám
 * của bệnh nhân.
 *
 * @param ghiChu            lời dặn của bác sĩ
 * @param ngayTaiKhamDeXuat gợi ý ngày tái khám, không phải lịch hẹn
 * @param donThuoc          rỗng nếu không kê đơn
 */
public record KetQuaKhamResponse(String chanDoan, String ghiChu, LocalDate ngayTaiKhamDeXuat, List<DonThuoc> donThuoc) {

    /** @param donVi đơn vị của thuốc trong danh mục */
    public record DonThuoc(String tenThuoc, String donVi, String lieuDung, Integer soLanMoiNgay, Integer soNgayDung,
            String ghiChuSuDung) {
    }
}
