package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

/** Tạo dữ liệu mẫu để thử / demo (chỉ có khi app.du-lieu-mau.bat = true). */
public interface DuLieuMauService {

    /** Tạo chuyên khoa, phòng khám, bác sĩ, bệnh nhân mẫu trong 1 transaction. */
    boolean taoDuLieuNen();

    /** Điền hồ sơ giới thiệu mẫu cho các bác sĩ mẫu CHƯA có giới thiệu ngắn. */
    int boSungHoSoBacSi();

    /** Điền bản tiếng Anh còn trống của dữ liệu mẫu, khi bản tiếng Việt vẫn đúng như dữ liệu mẫu. */
    int boSungTiengAnh();

    /** Điền giấy phép, ngôn ngữ khám, giải thưởng, nghiên cứu mẫu cho các bác sĩ mẫu CHƯA có ngôn ngữ khám. */
    int boSungGiayPhepVaNgonNgu();

    /** Điền triệu chứng thường gặp (2 ngôn ngữ) cho các chuyên khoa mẫu còn trống cả 2 bản. */
    int boSungTrieuChung();

    /**
     * Một lần trên DB tạo trước khi có ảnh mẫu thật (còn URL placehold.co): thay ảnh giữ chỗ bằng ảnh thật, điền ảnh đại
     * diện còn trống của tài khoản mẫu và thêm ảnh giới thiệu cho bác sĩ mẫu chưa từng có ảnh. Hết ảnh giữ chỗ thì không
     * làm gì. Trả số ảnh đại diện + số bác sĩ đã đổi.
     */
    int thayAnhMau();

    /**
     * Thêm ảnh giải thưởng mẫu (1 ảnh mỗi giải, chú thích là tên giải) cho bác sĩ mẫu có giải thưởng, chỉ khi bác sĩ chưa
     * có ảnh giải thưởng nào và chưa từng thêm ảnh nào sau dữ liệu mẫu (anh_id_cuoi = 3; bộ đếm chỉ tăng nên ảnh đã xoá
     * không bị thêm lại). Dùng cho cả DB mới lẫn DB cũ. Trả số bác sĩ được thêm ảnh.
     */
    int boSungAnhGiaiThuong();

    /** Thêm các thuốc mẫu còn thiếu trong danh mục (so theo tên + hàm lượng). */
    int boSungThuoc();

    /** Bổ sung ca làm việc + khung giờ còn thiếu của bác sĩ mẫu trong {@code soNgay} ngày từ {@code tuNgay}. */
    int boSungLichLamViec(LocalDate tuNgay);

}
