package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.booking.dto.request.KhaiBaoNgayNghiRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhaiBaoNgayNghiResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiResponse;

/**
 * Ngày bệnh viện nghỉ (Tết, lễ, nghỉ đột xuất). Trạng thái ca vẫn là nguồn duy nhất cho việc đặt lịch: khai báo 1 ngày
 * nghỉ hủy mọi ca còn hoạt động của ngày đó (lịch hẹn giữ nguyên, được đánh dấu cần đổi lịch, như khi quản trị viên hủy
 * ca), nên trang đặt lịch không còn lượt nào của ngày đó mà không cần lọc thêm. Sau đó không tạo ca vào ngày nghỉ được
 * (xem {@link QuanLyCaService}).
 */
public interface NgayNghiService {

    /**
     * Ngày nghỉ trong khoảng cho quản trị viên, theo ngày. Bỏ trống: từ hôm nay; tới {@code tuNgay} + 365 ngày. Khoảng
     * tối đa 366 ngày.
     */
    List<NgayNghiQuanTriResponse> danhSach(LocalDate tuNgay, LocalDate denNgay);

    /** Như {@link #danhSach} cho trang đặt lịch (không cần đăng nhập): ngày và lý do theo ngôn ngữ của request. */
    List<NgayNghiResponse> congKhai(LocalDate tuNgay, LocalDate denNgay);

    /**
     * Khai báo bệnh viện nghỉ [tuNgay, denNgay]: từ ngày mai trở đi (không thì 400), tối đa 42 ngày. Ngày đã nghỉ từ
     * trước được bỏ qua; mọi ngày đều đã nghỉ thì 409 NGAY_BENH_VIEN_NGHI. Mỗi ngày mới 1 dòng; mọi ca còn hoạt động của
     * các ngày mới bị hủy trong cùng transaction (lịch sử ca nguồn NGAY_NGHI, bệnh nhân được báo và nhận email), mỗi bác
     * sĩ bị hủy ca được báo 1 lần.
     *
     * @param luu false = xem trước: chạy y như khi lưu rồi rollback, không báo ai
     */
    KetQuaKhaiBaoNgayNghiResponse khaiBao(Long idTaiKhoanQuanTri, KhaiBaoNgayNghiRequest request, boolean luu);

    /**
     * Bỏ 1 ngày nghỉ chưa tới (hôm nay / đã qua thì 400; không có thì 404). Các ca đã bị hủy khi khai báo vẫn hủy; ngày
     * đó xếp ca lại được.
     */
    void xoa(Long idTaiKhoanQuanTri, LocalDate ngay);

}
