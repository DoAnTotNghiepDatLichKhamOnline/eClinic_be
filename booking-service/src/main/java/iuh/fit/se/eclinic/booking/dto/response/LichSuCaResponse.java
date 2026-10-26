package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.HanhDongCa;
import iuh.fit.se.eclinic.common.enums.NguonThayDoiCa;

/** 1 lần xếp / sửa / hủy 1 ca làm việc, cho quản trị viên. */
public record LichSuCaResponse(
        Long id,
        HanhDongCa hanhDong,
        NguonThayDoiCa nguon,
        LocalDateTime thoiDiem,
        QuanTriThucHien quanTri,
        Long idYeuCau,
        String lyDo,
        AnhCa truoc,
        AnhCa sau,
        List<String> cacTruongDoi) {

    public record QuanTriThucHien(Long id, String hoTen) {
    }

    /** Giá trị của ca tại 1 thời điểm. */
    public record AnhCa(
            BacSiTomTatResponse bacSi,
            PhongKhamTomTatResponse phongKham,
            LocalDate ngay,
            LocalTime gioBatDau,
            LocalTime gioKetThuc,
            Integer soLuotToiDaMoiGio,
            Integer thoiLuongLuotPhut) {
    }
}
