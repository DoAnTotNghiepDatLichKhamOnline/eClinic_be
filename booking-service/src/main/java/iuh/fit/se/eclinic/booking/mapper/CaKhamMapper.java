package iuh.fit.se.eclinic.booking.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.BacSiTomTatResponse;
import iuh.fit.se.eclinic.booking.dto.response.CaKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.KhungGioResponse;
import iuh.fit.se.eclinic.booking.dto.response.PhongKhamTomTatResponse;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

/**
 * Chuyển ca làm việc sang DTO xem khung giờ. Họ tên, ảnh đại diện bác sĩ lấy từ tài khoản của bác sĩ.
 */
@Component
public class CaKhamMapper {

    public CaKhamResponse toResponse(LichLamViec lichLamViec, List<KhungGioResponse> khungGio) {
        return new CaKhamResponse(lichLamViec.getId(), lichLamViec.getNgayLamViec(), lichLamViec.getGioBatDau(),
                lichLamViec.getGioKetThuc(), lichLamViec.getSoLuotToiDaMoiGio(), lichLamViec.getThoiLuongLuotPhut(),
                toBacSiTomTat(lichLamViec.getBacSi()), toPhongKhamTomTat(lichLamViec.getPhongKham()), khungGio);
    }

    public BacSiTomTatResponse toBacSiTomTat(BacSi bacSi) {
        return new BacSiTomTatResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(), bacSi.getHocVi(),
                bacSi.getTaiKhoan().getAnhDaiDien());
    }

    public PhongKhamTomTatResponse toPhongKhamTomTat(PhongKham phongKham) {
        return new PhongKhamTomTatResponse(phongKham.getId(), phongKham.getTenPhong(), phongKham.getTang());
    }

}
