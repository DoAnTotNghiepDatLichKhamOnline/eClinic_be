package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.HanhDongCa;
import iuh.fit.se.eclinic.common.enums.NguonThayDoiCa;

/**
 * 1 lần xếp / sửa / hủy 1 ca làm việc, cho quản trị viên.
 *
 * @param idYeuCau     yêu cầu đổi ca / xin nghỉ được duyệt (khi {@code nguon} = DUYET_YEU_CAU)
 * @param lyDo         lý do hủy theo ngôn ngữ của request (câu quản trị viên gõ giữ nguyên văn); null nếu không có
 * @param truoc        ca trước thay đổi; null ở dòng TAO
 * @param sau          ca sau thay đổi (ở dòng HUY: ca lúc bị hủy)
 * @param cacTruongDoi tên các trường của {@link AnhCa} khác giữa {@code truoc} và {@code sau}; rỗng ở dòng TAO / HUY
 */
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
