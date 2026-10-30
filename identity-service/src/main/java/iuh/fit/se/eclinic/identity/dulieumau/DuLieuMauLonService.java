package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

/** Thêm khoảng 100 dòng mỗi bảng để thử / demo (chỉ có khi app.du-lieu-mau.bat và .lon cùng bằng true). */
public interface DuLieuMauLonService {

    /** Tạo toàn bộ dữ liệu mẫu lớn trong 1 transaction; đã tạo rồi hoặc chưa có quản trị viên thì trả false. */
    boolean taoDuLieuLon();

    /** Bổ sung ca + khung giờ còn thiếu của các bác sĩ mẫu lớn trong {@code soNgay} ngày từ {@code tuNgay}. */
    int boSungLichLamViec(LocalDate tuNgay);

}
