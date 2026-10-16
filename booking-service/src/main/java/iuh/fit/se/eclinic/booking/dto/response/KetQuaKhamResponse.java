package iuh.fit.se.eclinic.booking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;

/**
 * Kết quả 1 lượt khám (đọc từ hồ sơ bệnh án của medical-service), dùng chung cho hồ sơ khám của bác sĩ và lịch sử khám
 * của bệnh nhân.
 *
 * @param lyDoKham          lý do khám, triệu chứng bác sĩ ghi
 * @param sinhHieu          null nếu chưa đo chỉ số nào
 * @param khamLamSang       kết quả khám lâm sàng
 * @param chanDoan          chẩn đoán chính
 * @param icdChinh          mã ICD-10 của chẩn đoán chính; null nếu bác sĩ không chọn
 * @param chanDoanKemTheo   các bệnh kèm theo
 * @param ghiChu            lời dặn của bác sĩ
 * @param ngayTaiKhamDeXuat gợi ý ngày tái khám, không phải lịch hẹn
 * @param donThuoc          rỗng nếu không kê đơn
 * @param ngaySuaCuoi       bác sĩ sửa hồ sơ bệnh án lần gần nhất lúc này; null = chưa sửa
 * @param phienBan          CHỈ cho bác sĩ (hồ sơ khám): phiên bản gửi lại khi sửa; không có trong phản hồi cho bệnh nhân
 * @param hanSua            CHỈ cho bác sĩ: sau thời điểm này hồ sơ bệnh án chỉ xem được
 */
public record KetQuaKhamResponse(String lyDoKham, SinhHieu sinhHieu, String khamLamSang, String chanDoan,
        IcdChinh icdChinh, String chanDoanKemTheo, String ghiChu, LocalDate ngayTaiKhamDeXuat, List<DonThuoc> donThuoc,
        LocalDateTime ngaySuaCuoi,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer phienBan,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime hanSua) {

    /** Mạch, nhịp thở lần/phút; nhiệt độ °C; huyết áp mmHg; cân nặng kg; chiều cao cm. Chỉ số không đo là null. */
    public record SinhHieu(Integer mach, BigDecimal nhietDo, Integer huyetApTamThu, Integer huyetApTamTruong,
            Integer nhipTho, BigDecimal canNang, BigDecimal chieuCao) {
    }

    /**
     * @param ten     theo ngôn ngữ người xem
     * @param manTinh bệnh mạn tính theo Thông tư 26/2025 (thuốc kê được tối đa 90 ngày)
     */
    public record IcdChinh(String ma, String ten, boolean manTinh) {
    }

    /**
     * @param idThuoc  thuốc trong danh mục; bác sĩ gửi lại id này khi sửa hồ sơ bệnh án
     * @param hoatChat null ở thuốc cũ chưa được quản trị viên bổ sung (cả {@code hamLuong}, {@code duongDung})
     * @param donVi    đơn vị của thuốc trong danh mục
     * @param soLuong  tổng số lượng cấp, theo {@code donVi}; null ở dòng ghi trước khi bắt buộc số lượng
     */
    public record DonThuoc(Long idThuoc, String tenThuoc, String hoatChat, String hamLuong, DuongDungThuoc duongDung, String donVi,
            String lieuDung, Integer soLanMoiNgay, Integer soNgayDung, Integer soLuong, String ghiChuSuDung) {
    }
}
