package iuh.fit.se.eclinic.booking.service;

/**
 * Báo và nhắc ngày tái khám bác sĩ đề xuất trong hồ sơ bệnh án (xem {@code NhacTaiKhamJob}). KHÔNG tạo lịch hẹn: bệnh
 * nhân tự đặt nếu muốn.
 * <p>
 * medical-service chỉ ghi ngày tái khám; ở đây so ngày đó với ngày đã báo cho bệnh nhân (bảng nhac_tai_kham):
 * <ol>
 * <li>Báo: ngày tái khám ở sau hôm nay và khác ngày đã báo thì báo "bác sĩ hẹn tái khám", hoặc "đổi ngày tái khám" nếu
 * trước đó đã báo 1 ngày khác. Ngày hẹn đã nằm trong khoảng nhắc trước thì coi như đã nhắc.</li>
 * <li>Nhắc: còn {@code app.nhac-tai-kham.truoc-ngay} ngày trở xuống thì nhắc 1 lần. Bỏ qua lần nhắc này nếu hồ sơ bệnh
 * nhân đó đang có lịch hẹn sắp tới còn hiệu lực với bác sĩ cùng chuyên khoa.</li>
 * <li>Quên: bác sĩ xoá ngày tái khám thì không gửi gì và không nhắc nữa.</li>
 * </ol>
 * Người nhận và ngôn ngữ lấy theo lịch hẹn của lượt khám đó, như email xác nhận đặt lịch.
 */
public interface NhacTaiKhamService {

    /**
     * Báo và nhắc các ngày tái khám đến hạn. Ngoài giờ gửi ({@code app.nhac-lich.gui-tu} - {@code gui-den}) không làm gì.
     *
     * @return số hồ sơ bệnh án vừa được báo hoặc nhắc
     */
    int xuLyDenHan();

}
