package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

import iuh.fit.se.eclinic.booking.dto.response.HangDoiResponse;

/**
 * Hàng đợi khám trong ngày của CHÍNH bác sĩ đang đăng nhập, theo ca. Gồm lịch hẹn CHO_XAC_NHAN / DA_XAC_NHAN /
 * DA_HOAN_THANH (đã khám) của các ca đang hoạt động, trừ lịch hẹn cần đổi lịch. Lỗi tài khoản như
 * {@link TaiKhoanService#layBacSiDangHoatDong}.
 * <p>
 * Các thao tác chỉ làm được trong ngày khám (KHONG_PHAI_NGAY_KHAM), với lịch hẹn còn khám được
 * (LICH_HEN_KHONG_KHAM_DUOC). Lịch hẹn / ca không có hoặc của bác sĩ khác: KHONG_TIM_THAY. Mỗi thao tác trả lại hàng
 * đợi hôm nay.
 */
public interface HangDoiService {

    /** Hàng đợi của 1 ngày (null = hôm nay). */
    HangDoiResponse hangDoi(Long idTaiKhoanBacSi, LocalDate ngay);

    /** Gọi lượt CHO_KHAM có số thứ tự nhỏ nhất của 1 ca hôm nay; không còn ai chờ: HANG_DOI_TRONG. */
    HangDoiResponse goiTiep(Long idTaiKhoanBacSi, Long idLichLamViec);

    /**
     * CHO_KHAM / VANG_MAT -> DANG_KHAM. Gọi lại bệnh nhân đang khám thì không đổi gì; đang khám bệnh nhân khác chưa ghi
     * nhận kết quả: DANG_KHAM_BENH_NHAN_KHAC.
     */
    HangDoiResponse goi(Long idTaiKhoanBacSi, Long idLichHen);

    /** CHO_KHAM / DANG_KHAM -> VANG_MAT; đã vắng mặt thì không đổi gì. */
    HangDoiResponse vangMat(Long idTaiKhoanBacSi, Long idLichHen);

}
