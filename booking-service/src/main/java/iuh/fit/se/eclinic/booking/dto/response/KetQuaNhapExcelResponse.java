package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Kết quả nhập lịch từ Excel (xem trước hoặc đã lưu). Nhập theo kiểu tất cả hoặc không: chỉ lưu khi {@code loi} rỗng.
 *
 * @param daLuu false = chỉ xem trước, hoặc tệp còn lỗi: chưa ca nào được lưu
 * @param soDong số dòng ca trong tệp (không tính tiêu đề và dòng trống)
 * @param hopLe  các dòng không có lỗi, theo thứ tự dòng
 * @param loi    các lỗi theo dòng rồi theo cột; 1 dòng có thể có nhiều lỗi ô
 */
public record KetQuaNhapExcelResponse(boolean daLuu, int soDong, List<DongHopLe> hopLe, List<LoiDong> loi) {

    /** @param idCaMoi id ca vừa tạo; null khi chưa lưu */
    public record DongHopLe(int dong, Long idCaMoi, BacSiTomTatResponse bacSi, PhongKhamTomTatResponse phongKham,
            LocalDate ngay, LocalTime gioBatDau, LocalTime gioKetThuc) {
    }

    /**
     * @param cot  chữ cái của cột (A-G); null khi lỗi của cả dòng (trùng giờ, vượt giờ làm trong tuần...)
     * @param lyDo câu theo ngôn ngữ của request
     */
    public record LoiDong(int dong, String cot, String lyDo) {
    }
}
