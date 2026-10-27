package iuh.fit.se.eclinic.medical.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.TruongBenhAn;

/** Lịch sử sửa hồ sơ bệnh án của 1 lượt khám, cho bác sĩ mở được hồ sơ khám của bệnh nhân và quản trị viên. */
public record LichSuSuaBenhAnResponse(Long idLichHen, String maTraCuu, int phienBanHienTai, LocalDateTime ngayGhiNhan,
        List<LanSua> cacLanSua) {

    public record LanSua(int phienBan, LocalDateTime ngaySua, BacSiSua bacSi, String lyDo,
            List<TruongBenhAn> cacTruongDoi, NoiDungBenhAn truoc, NoiDungBenhAn sau) {
    }

    public record BacSiSua(Long id, String hoTen) {
    }
}
