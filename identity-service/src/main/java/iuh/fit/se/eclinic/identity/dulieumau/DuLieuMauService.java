package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

/**
 * Tạo dữ liệu mẫu để thử / demo (chỉ có khi app.du-lieu-mau.bat = true).
 * <p>
 * NGOẠI LỆ của quy tắc "service chỉ ghi bảng của mình": identity-service ghi cả bảng của catalog (chuyên khoa,
 * phòng khám, bác sĩ) và booking (hồ sơ bệnh nhân, ca làm việc, khung giờ). Lý do: ca làm việc cần admin, bác sĩ,
 * phòng khám có trước, mà các service khởi động song song nên không thể chia cho từng service tự tạo.
 */
public interface DuLieuMauService {

    /**
     * Tạo chuyên khoa, phòng khám, bác sĩ, bệnh nhân mẫu trong 1 transaction. Chỉ chạy khi DB chưa có tài khoản
     * bác sĩ nào, nên sau lần đầu không tạo lại và không ghi đè những gì người dùng đã sửa / xoá.
     *
     * @return false nếu đã có bác sĩ (không ghi gì)
     */
    boolean taoDuLieuNen();

    /**
     * Bổ sung ca làm việc + khung giờ còn thiếu của các bác sĩ mẫu trong {@code soNgay} ngày tính từ {@code tuNgay}.
     * Bỏ qua ngày bác sĩ đã có ca (kể cả ca đã huỷ) và ca trùng giờ với ca đang hoạt động của phòng.
     *
     * @return số ca đã tạo
     */
    int boSungLichLamViec(LocalDate tuNgay);

}
