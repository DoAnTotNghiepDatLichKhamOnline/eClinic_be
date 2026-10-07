package iuh.fit.se.eclinic.booking.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.config.DanhGiaProperties;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaBacSiResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaQuanTriResponse;
import iuh.fit.se.eclinic.common.entity.booking.DanhGia;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DanhGiaMapper {

    private final DanhGiaProperties danhGiaProperties;

    public DanhGiaCuaToiResponse toCuaToi(DanhGia danhGia) {
        return new DanhGiaCuaToiResponse(danhGia.getSoSao(), danhGia.getNhanXet(), danhGia.getNgayTao(),
                danhGia.getNgayTao().plusDays(danhGiaProperties.soNgayDuocSua()));
    }

    /** Ẩn danh với bác sĩ: không đưa tên bệnh nhân, mã lịch hẹn. */
    public DanhGiaCuaBacSiResponse toCuaBacSi(DanhGia danhGia) {
        return new DanhGiaCuaBacSiResponse(danhGia.getSoSao(), danhGia.getNhanXet(),
                danhGia.getLichHen().getKhungGio().getGioBatDau().toLocalDate(), danhGia.getNgayTao());
    }

    public DanhGiaQuanTriResponse toQuanTri(DanhGia danhGia) {
        LichHen lichHen = danhGia.getLichHen();
        return new DanhGiaQuanTriResponse(danhGia.getId(), danhGia.getSoSao(), danhGia.getNhanXet(),
                danhGia.getNgayTao(), danhGia.getNgayCapNhat(), lichHen.getMaTraCuu(),
                lichHen.getKhungGio().getGioBatDau().toLocalDate(), lichHen.getHoSoBenhNhan().getId(),
                lichHen.getHoSoBenhNhan().getHoTen(), danhGia.getBacSi().getId(),
                danhGia.getBacSi().getTaiKhoan().getHoTen());
    }
}
