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
     * Điền hồ sơ giới thiệu mẫu (chức vụ, giới thiệu ngắn, đào tạo, công tác, lĩnh vực khám chữa, ảnh đại diện và 3 ảnh
     * giới thiệu giữ chỗ) cho các bác sĩ mẫu CHƯA có giới thiệu ngắn. Chạy mỗi lần khởi động nên DB đã có dữ liệu mẫu
     * từ trước cũng được bổ sung; bác sĩ đã được quản trị viên sửa hồ sơ thì không bị ghi đè.
     *
     * @return số bác sĩ được bổ sung
     */
    int boSungHoSoBacSi();

    /**
     * Điền bản tiếng Anh cho chuyên khoa, phòng khám, hồ sơ và ảnh giới thiệu của bác sĩ mẫu, ở những chỗ bản tiếng Anh
     * còn trống VÀ bản tiếng Việt vẫn đúng như dữ liệu mẫu (nội dung quản trị viên đã sửa thì không điền, để 2 bản
     * không lệch nhau). Chạy mỗi lần khởi động nên DB có dữ liệu mẫu từ trước cũng được bổ sung.
     *
     * @return số chuyên khoa, phòng khám, bác sĩ được bổ sung
     */
    int boSungTiengAnh();

    /**
     * Điền giấy phép hành nghề (ngày cấp, nơi cấp, phạm vi), ngôn ngữ khám, giải thưởng và công trình nghiên cứu mẫu (cả
     * bản tiếng Anh) cho các bác sĩ mẫu CHƯA có ngôn ngữ khám. Chạy mỗi lần khởi động nên DB có dữ liệu mẫu từ trước cũng
     * được bổ sung; giải thưởng / nghiên cứu đã nhập thì giữ nguyên.
     *
     * @return số bác sĩ được bổ sung
     */
    int boSungGiayPhepVaNgonNgu();

    /**
     * Điền triệu chứng thường gặp (2 ngôn ngữ) cho các chuyên khoa mẫu còn trống cả 2 bản, để chatbot gợi ý được chuyên
     * khoa. Chạy mỗi lần khởi động; chuyên khoa quản trị viên đã nhập thì không đụng tới.
     *
     * @return số chuyên khoa được bổ sung
     */
    int boSungTrieuChung();

    /**
     * Thêm các thuốc mẫu (đã xác minh, đang dùng) còn thiếu trong danh mục, so theo tên + hàm lượng, để bác sĩ mẫu kê đơn
     * được. Chạy mỗi lần khởi động; thuốc đã có (kể cả đã bị sửa / ngừng dùng) thì không đụng tới.
     *
     * @return số thuốc đã thêm
     */
    int boSungThuoc();

    /**
     * Bổ sung ca làm việc + khung giờ còn thiếu của các bác sĩ mẫu trong {@code soNgay} ngày tính từ {@code tuNgay}.
     * Bỏ qua ngày bác sĩ đã có ca (kể cả ca đã huỷ) và ca trùng giờ với ca đang hoạt động của phòng.
     *
     * @return số ca đã tạo
     */
    int boSungLichLamViec(LocalDate tuNgay);

}
