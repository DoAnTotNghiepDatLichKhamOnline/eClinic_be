package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.booking.dto.response.CaKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.KhungGioGopResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayConChoResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgaySomNhatResponse;

/**
 * Xem khung giờ khám khi đặt lịch (SCHED-04, BOOK-12). Chỉ đọc.
 * <p>
 * Lọc theo {@code idBacSi} hoặc {@code idChuyenKhoa} (mọi bác sĩ của chuyên khoa); có cả hai thì dùng idBacSi,
 * thiếu cả hai thì ném DU_LIEU_KHONG_HOP_LE. Chỉ tính ca còn hoạt động của bác sĩ đang công tác có tài khoản
 * đã kích hoạt; bác sĩ không tồn tại / không còn làm việc cho kết quả rỗng.
 * <p>
 * 1 lượt khám đặt được khi còn trống, bắt đầu sau hiện tại ít nhất {@code app.dat-lich.dat-truoc-toi-thieu}
 * và nằm trong {@code app.dat-lich.so-ngay-dat-truoc-toi-da} ngày tới.
 */
public interface TraCuuLichKhamService {

    /**
     * Các ca của ngày kèm khung 1 giờ. Khung không còn lượt nào kịp đặt thì bỏ, ca không còn khung nào thì bỏ;
     * khung còn kịp đặt nhưng đã kín vẫn trả về với {@code hetCho = true}.
     */
    List<CaKhamResponse> timKhungGioTheoNgay(LocalDate ngay, Long idBacSi, Long idChuyenKhoa);

    /**
     * Các ngày có ca trong khoảng, kèm số chỗ còn lại. Khoảng ngày bị cắt về [hôm nay, hôm nay + số ngày đặt trước
     * tối đa]; null = lấy hết khoảng đó. Ném DU_LIEU_KHONG_HOP_LE nếu denNgay trước tuNgay.
     */
    List<NgayConChoResponse> timNgayConCho(LocalDate tuNgay, LocalDate denNgay, Long idBacSi, Long idChuyenKhoa);

    /**
     * Như {@link #timKhungGioTheoNgay} cho 1 bác sĩ trên nhiều ngày liền (tối đa 7), theo ngày rồi giờ bắt đầu ca.
     * tuNgay null = hôm nay, denNgay null = tuNgay + 6. Khoảng ngày bị cắt về khoảng còn đặt được. Ném
     * DU_LIEU_KHONG_HOP_LE nếu denNgay trước tuNgay hoặc khoảng dài hơn 7 ngày.
     */
    List<CaKhamResponse> timKhungGioNhieuNgay(Long idBacSi, LocalDate tuNgay, LocalDate denNgay);

    /**
     * Khung giờ của chuyên khoa trong 1 ngày, gộp các khung cùng giờ bắt đầu của mọi bác sĩ (cho "bác sĩ bất kỳ"),
     * theo giờ bắt đầu. Khung đã kín vẫn trả về với {@code hetCho = true}.
     */
    List<KhungGioGopResponse> timKhungGioGop(Long idChuyenKhoa, LocalDate ngay);

    /**
     * Ngày còn chỗ sớm nhất của từng bác sĩ thuộc chuyên khoa, trong [hôm nay, hôm nay + số ngày đặt trước tối đa],
     * sắp theo id bác sĩ. Bác sĩ không còn ngày nào còn chỗ thì không có trong kết quả.
     */
    List<NgaySomNhatResponse> timNgaySomNhat(Long idChuyenKhoa);

}
