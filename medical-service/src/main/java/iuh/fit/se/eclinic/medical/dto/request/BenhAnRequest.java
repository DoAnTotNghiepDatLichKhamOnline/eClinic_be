package iuh.fit.se.eclinic.medical.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Kết quả 1 lượt khám do bác sĩ ghi (EXAM-01, EXAM-02). Trường không bắt buộc: bỏ trống hoặc gửi chuỗi rỗng đều được.
 *
 * @param lyDoKham          lý do khám, triệu chứng; màn hình khám điền sẵn lý do bệnh nhân ghi khi đặt lịch
 * @param sinhHieu          bỏ trống = không đo; trẻ dưới 72 tháng có kê thuốc thì bắt buộc cân nặng (kiểm tra trong
 *                          service)
 * @param khamLamSang       kết quả khám lâm sàng
 * @param chanDoan          chẩn đoán chính, chữ tự do
 * @param maIcdChinh        mã ICD-10 của chẩn đoán chính trong danh mục (gợi ý: GET /api/medical/bac-si/toi/icd10), không
 *                          phân biệt hoa thường; mã mạn tính cho kê mỗi thuốc tối đa 90 ngày thay vì 30
 * @param chanDoanKemTheo   các bệnh kèm theo, chữ tự do
 * @param ngayTaiKhamDeXuat gợi ý ngày tái khám, không tự tạo lịch hẹn; phải sau hôm nay, trừ khi sửa mà giữ nguyên ngày cũ
 *                          (kiểm tra trong service)
 * @param donThuoc          bỏ trống = không kê đơn; khi sửa, danh sách này thay toàn bộ đơn thuốc cũ
 * @param lyDoSua           chỉ khi sửa (bắt buộc): lý do, lưu vào lịch sử sửa
 * @param phienBan          chỉ khi sửa (bắt buộc): phiên bản hồ sơ đang xem; đã có người sửa trước thì 409
 */
public record BenhAnRequest(
        @Size(max = 2000, message = "{kiemTra.lyDoKham.toiDa}")
        String lyDoKham,

        @Valid
        SinhHieuRequest sinhHieu,

        @Size(max = 5000, message = "{kiemTra.khamLamSang.toiDa}")
        String khamLamSang,

        @NotBlank(message = "{kiemTra.chanDoan.batBuoc}")
        @Size(max = 5000, message = "{kiemTra.chanDoan.toiDa}")
        String chanDoan,

        @Size(max = 10, message = "{kiemTra.maIcdChinh.toiDa}")
        String maIcdChinh,

        @Size(max = 2000, message = "{kiemTra.chanDoanKemTheo.toiDa}")
        String chanDoanKemTheo,

        @Size(max = 5000, message = "{kiemTra.ghiChu.toiDa}")
        String ghiChu,

        LocalDate ngayTaiKhamDeXuat,

        @Valid
        @Size(max = 30, message = "{kiemTra.donThuoc.toiDa}")
        List<DonThuocRequest> donThuoc,

        @NotBlank(groups = BenhAnRequest.KhiSua.class, message = "{kiemTra.lyDoSua.batBuoc}")
        @Size(max = 500, groups = BenhAnRequest.KhiSua.class, message = "{kiemTra.lyDoSua.toiDa}")
        String lyDoSua,

        @NotNull(groups = BenhAnRequest.KhiSua.class, message = "{kiemTra.phienBan.batBuoc}")
        Integer phienBan) {

    /** Nhóm kiểm tra chỉ dùng khi sửa hồ sơ bệnh án (PUT). */
    public interface KhiSua {
    }
}
