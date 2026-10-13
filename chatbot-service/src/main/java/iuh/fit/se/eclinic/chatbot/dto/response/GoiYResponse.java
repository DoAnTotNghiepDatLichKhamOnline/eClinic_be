package iuh.fit.se.eclinic.chatbot.dto.response;

import java.util.List;

/**
 * Gợi ý đi kèm 1 câu trả lời của chatbot, do SERVER dựng từ kết quả tra cứu dữ liệu phòng khám lúc trả lời (không lấy
 * từ câu chữ của mô hình): frontend vẽ thẻ bác sĩ, khung giờ từ đây và dùng các id để mở form đặt lịch. Là ảnh chụp tại
 * lúc trả lời: số chỗ còn lại có thể đã đổi, form đặt lịch phải đọc lại khung giờ. Chữ theo ngôn ngữ của request đã tạo
 * ra câu trả lời.
 *
 * @param chuyenKhoa chuyên khoa được gợi ý; null khi không xác định
 * @param bacSi      bác sĩ của chuyên khoa, người còn chỗ sớm nhất đứng trước; rỗng khi chưa tra cứu bác sĩ
 * @param lichKham   các ca còn chỗ của 1 bác sĩ trong tối đa 7 ngày; rỗng khi chưa tra cứu lịch
 * @param lichHenCuaToi lịch hẹn sắp tới của CHÍNH bệnh nhân đang chat (tối đa 10, gần nhất trước); null khi câu trả lời
 *                      không tra cứu phần này, rỗng khi đã tra cứu mà không có lịch nào
 * @param lanKham       các lượt đã khám của chính bệnh nhân đó (tối đa 10, mới nhất trước); null / rỗng như trên
 */
public record GoiYResponse(ChuyenKhoa chuyenKhoa, List<BacSi> bacSi, List<LichKham> lichKham,
        List<LichHenCuaToi> lichHenCuaToi, List<LanKham> lanKham) {

    /** Câu trả lời này có kèm dữ liệu cá nhân của người chat (kể cả khi tra cứu ra danh sách rỗng). */
    public boolean coDuLieuCaNhan() {
        return lichHenCuaToi != null || lanKham != null;
    }

    public record ChuyenKhoa(Long id, String ten) {
    }

    /**
     * @param ngaySomNhat ngày còn chỗ sớm nhất (yyyy-MM-dd); null khi bác sĩ không còn chỗ trong khoảng đặt lịch được
     * @param soChoConLai số chỗ còn lại của ngày đó; 0 khi {@code ngaySomNhat} null
     */
    public record BacSi(Long id, String hoTen, String hocVi, String chucVu, String anhDaiDien, String ngaySomNhat,
            long soChoConLai) {
    }

    /**
     * 1 ca làm việc còn chỗ. Đặt lịch bằng {@code idLichLamViec} + {@code khungGio[].gioBatDau}.
     *
     * @param ngay yyyy-MM-dd
     */
    public record LichKham(Long idBacSi, String hoTenBacSi, Long idLichLamViec, String ngay, String tenPhong,
            String tang, List<KhungGio> khungGio) {
    }

    /**
     * 1 lịch hẹn sắp tới của người chat, như 1 dòng "lịch hẹn của tôi" thu gọn. Mở chi tiết bằng {@code maPhieuKham}
     * (GET /api/booking/lich-hen/cua-toi/{maPhieuKham}) hoặc {@code linkPhieuKham}. Họ tên và các mã chỉ có ở đây, mô
     * hình không nhận được.
     *
     * @param trangThai  CHO_XAC_NHAN hoặc DA_XAC_NHAN
     * @param laBanThan  false: lịch tài khoản đặt cho người khác
     * @param canDoiLich true: ca khám đã bị hủy, lịch đang chờ bệnh nhân đổi sang giờ khác hoặc hủy
     */
    public record LichHenCuaToi(String maPhieuKham, String maTraCuu, String linkPhieuKham, String trangThai, String ngay,
            String gioBatDauKhung, String gioKetThucKhung, String hoTenBacSi, String tenChuyenKhoa, String tenPhong,
            String tang, String hoTenBenhNhan, boolean laBanThan, boolean canDoiLich) {
    }

    /**
     * 1 lượt đã khám của người chat. Chẩn đoán, lời dặn, đơn thuốc KHÔNG có ở đây: mở lịch sử khám bằng
     * {@code maPhieuKham}.
     *
     * @param ngayTaiKhamDeXuat yyyy-MM-dd; null khi bác sĩ không hẹn tái khám. Chỉ là gợi ý, không phải lịch hẹn
     */
    public record LanKham(String maPhieuKham, String maTraCuu, String ngay, String hoTenBacSi, String tenChuyenKhoa,
            String hoTenBenhNhan, boolean laBanThan, String ngayTaiKhamDeXuat) {
    }

    /** Khung 1 giờ còn chỗ; giờ dạng ISO như API khung giờ ({@code 2026-10-14T08:00:00}). */
    public record KhungGio(String gioBatDau, String gioKetThuc, int soChoConLai) {
    }

}
