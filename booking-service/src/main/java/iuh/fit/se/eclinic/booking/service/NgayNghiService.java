package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.booking.dto.request.KhaiBaoNgayNghiRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhaiBaoNgayNghiResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiResponse;

/** Ngày bệnh viện nghỉ (Tết, lễ, nghỉ đột xuất). */
public interface NgayNghiService {

    /** Ngày nghỉ trong khoảng cho quản trị viên, theo ngày. */
    List<NgayNghiQuanTriResponse> danhSach(LocalDate tuNgay, LocalDate denNgay);

    /** Như {@link #danhSach} cho trang đặt lịch (không cần đăng nhập): ngày và lý do theo ngôn ngữ của request. */
    List<NgayNghiResponse> congKhai(LocalDate tuNgay, LocalDate denNgay);

    /** Khai báo bệnh viện nghỉ [tuNgay, denNgay]: từ ngày mai trở đi (không thì 400), tối đa 42 ngày. */
    KetQuaKhaiBaoNgayNghiResponse khaiBao(Long idTaiKhoanQuanTri, KhaiBaoNgayNghiRequest request, boolean luu);

    /** Bỏ 1 ngày nghỉ chưa tới (hôm nay / đã qua thì 400; không có thì 404). */
    void xoa(Long idTaiKhoanQuanTri, LocalDate ngay);

}
