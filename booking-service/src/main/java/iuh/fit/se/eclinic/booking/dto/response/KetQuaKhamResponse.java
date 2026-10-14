package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;

/**
 * Kết quả 1 lượt khám (đọc từ hồ sơ bệnh án của medical-service), dùng chung cho hồ sơ khám của bác sĩ và lịch sử khám
 * của bệnh nhân.
 *
 * @param ghiChu            lời dặn của bác sĩ
 * @param ngayTaiKhamDeXuat gợi ý ngày tái khám, không phải lịch hẹn
 * @param donThuoc          rỗng nếu không kê đơn
 * @param ngaySuaCuoi       bác sĩ sửa hồ sơ bệnh án lần gần nhất lúc này; null = chưa sửa
 * @param phienBan          CHỈ cho bác sĩ (hồ sơ khám): phiên bản gửi lại khi sửa; không có trong phản hồi cho bệnh nhân
 * @param hanSua            CHỈ cho bác sĩ: sau thời điểm này hồ sơ bệnh án chỉ xem được
 */
public record KetQuaKhamResponse(String chanDoan, String ghiChu, LocalDate ngayTaiKhamDeXuat, List<DonThuoc> donThuoc,
        LocalDateTime ngaySuaCuoi,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer phienBan,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime hanSua) {

    /**
     * @param idThuoc  thuốc trong danh mục; bác sĩ gửi lại id này khi sửa hồ sơ bệnh án
     * @param hoatChat null ở thuốc cũ chưa được quản trị viên bổ sung (cả {@code hamLuong}, {@code duongDung})
     * @param donVi    đơn vị của thuốc trong danh mục
     */
    public record DonThuoc(Long idThuoc, String tenThuoc, String hoatChat, String hamLuong, DuongDungThuoc duongDung, String donVi,
            String lieuDung, Integer soLanMoiNgay, Integer soNgayDung, String ghiChuSuDung) {
    }
}
