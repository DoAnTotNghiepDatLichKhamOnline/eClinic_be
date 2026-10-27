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

    /** Thêm các thuốc mẫu còn thiếu trong danh mục (so theo tên + hàm lượng). */
    int boSungThuoc();

    /** Bổ sung ca làm việc + khung giờ còn thiếu của bác sĩ mẫu trong {@code soNgay} ngày từ {@code tuNgay}. */
    int boSungLichLamViec(LocalDate tuNgay);

}
