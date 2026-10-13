package iuh.fit.se.eclinic.medical.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Kết quả 1 lượt khám do bác sĩ ghi (EXAM-01, EXAM-02). Trường không bắt buộc: bỏ trống hoặc gửi chuỗi rỗng đều được.
 *
 * @param ngayTaiKhamDeXuat gợi ý ngày tái khám, không tự tạo lịch hẹn
 * @param donThuoc          bỏ trống = không kê đơn; khi sửa, danh sách này thay toàn bộ đơn thuốc cũ
 */
public record BenhAnRequest(
        @NotBlank(message = "{kiemTra.chanDoan.batBuoc}")
        @Size(max = 5000, message = "{kiemTra.chanDoan.toiDa}")
        String chanDoan,

        @Size(max = 5000, message = "{kiemTra.ghiChu.toiDa}")
        String ghiChu,

        @Future(message = "{kiemTra.ngayTaiKham.sauHomNay}")
        LocalDate ngayTaiKhamDeXuat,

        @Valid
        @Size(max = 30, message = "{kiemTra.donThuoc.toiDa}")
        List<DonThuocRequest> donThuoc) {
}
