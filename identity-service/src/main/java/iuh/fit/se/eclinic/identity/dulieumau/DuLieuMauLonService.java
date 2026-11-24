package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

/** Thêm khoảng 100 dòng mỗi bảng để thử / demo (chỉ có khi app.du-lieu-mau.bat và .lon cùng bằng true). */
public interface DuLieuMauLonService {

    /** Tạo toàn bộ dữ liệu mẫu lớn trong 1 transaction; đã tạo rồi hoặc chưa có quản trị viên thì trả false. */
    boolean taoDuLieuLon();

    /** Bổ sung ca + khung giờ còn thiếu của các bác sĩ mẫu lớn từ {@code tuNgay} tới {@link DuLieuMauProperties#denNgayXepCa}. */
    int boSungLichLamViec(LocalDate tuNgay);

    /**
     * Thêm lần khám đã hoàn thành (kèm hồ sơ bệnh án, đơn thuốc, ngày tái khám) để mỗi hồ sơ bệnh nhân mẫu
     * ({@code benhnhanNN@eclinic.local}) có đủ 10 hồ sơ bệnh án; hồ sơ đã đủ thì bỏ qua. Hồ sơ bệnh án cũ chưa có ngày tái
     * khám và chưa từng sửa được thêm ngày tái khám. Trả số lần khám đã thêm.
     */
    int boSungLichSuKham(LocalDate homNay);

    /**
     * Mỗi ca đang hoạt động từ {@code homNay} của bác sĩ mẫu ({@code bacsiNN@eclinic.local}) có đủ 1 nửa
     * {@code so_benh_nhan_toi_da} lịch hẹn còn hiệu lực (tính cả lịch đã có), bằng lịch đã xác nhận của các hồ sơ khách
     * không tài khoản; nửa còn lại để thử đặt lịch. Lịch thêm không nhắc lịch. Trả số lịch hẹn đã thêm.
     */
    int lapNuaCaSapToi(LocalDate homNay);

    /**
     * Nâng {@code so_ngay_dat_truoc_toi_da} để đặt được lịch tới {@code app.du-lieu-mau.den-ngay}, ghi lịch sử như 1 lần
     * quản trị viên lưu; không bao giờ hạ. Chưa có dòng cấu hình thì bỏ qua. Trả số ngày mới, 0 nếu không đổi.
     */
    int nangHanDatLich(LocalDate homNay);

}
