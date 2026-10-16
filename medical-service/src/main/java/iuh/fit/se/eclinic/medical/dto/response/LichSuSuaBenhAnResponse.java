package iuh.fit.se.eclinic.medical.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.TruongBenhAn;

/**
 * Lịch sử sửa hồ sơ bệnh án của 1 lượt khám, cho bác sĩ mở được hồ sơ khám của bệnh nhân và quản trị viên.
 *
 * @param phienBanHienTai 1 = chưa sửa lần nào
 * @param ngayGhiNhan     lúc ghi nhận kết quả khám (phiên bản 1)
 * @param cacLanSua       mới nhất trước; rỗng nếu chưa sửa lần nào
 */
public record LichSuSuaBenhAnResponse(Long idLichHen, String maTraCuu, int phienBanHienTai, LocalDateTime ngayGhiNhan,
        List<LanSua> cacLanSua) {

    /**
     * @param phienBan     phiên bản hồ sơ sau lần sửa này (2, 3, ...)
     * @param cacTruongDoi phần đã đổi; {@code truoc} / {@code sau} giữ toàn bộ nội dung để so
     */
    public record LanSua(int phienBan, LocalDateTime ngaySua, BacSiSua bacSi, String lyDo,
            List<TruongBenhAn> cacTruongDoi, NoiDungBenhAn truoc, NoiDungBenhAn sau) {
    }

    public record BacSiSua(Long id, String hoTen) {
    }
}
