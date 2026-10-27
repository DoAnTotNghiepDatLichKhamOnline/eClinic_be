package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

import iuh.fit.se.eclinic.booking.dto.response.HangDoiResponse;

/** Hàng đợi khám trong ngày của CHÍNH bác sĩ đang đăng nhập, theo ca. */
public interface HangDoiService {

    /** Hàng đợi của 1 ngày (null = hôm nay). */
    HangDoiResponse hangDoi(Long idTaiKhoanBacSi, LocalDate ngay);

    /** Gọi lượt CHO_KHAM có số thứ tự nhỏ nhất của 1 ca hôm nay; không còn ai chờ: HANG_DOI_TRONG. */
    HangDoiResponse goiTiep(Long idTaiKhoanBacSi, Long idLichLamViec);

    /** CHO_KHAM / VANG_MAT -> DANG_KHAM. */
    HangDoiResponse goi(Long idTaiKhoanBacSi, Long idLichHen);

    /** CHO_KHAM / DANG_KHAM -> VANG_MAT; đã vắng mặt thì không đổi gì. */
    HangDoiResponse vangMat(Long idTaiKhoanBacSi, Long idLichHen);

}
