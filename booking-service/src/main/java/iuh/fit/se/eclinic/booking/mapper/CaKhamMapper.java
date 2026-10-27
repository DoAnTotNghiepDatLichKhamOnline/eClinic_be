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
import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Chuyển ca làm việc sang DTO xem khung giờ. */
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

    /** Tên phòng, tầng theo ngôn ngữ của request. */
    public PhongKhamTomTatResponse toPhongKhamTomTat(PhongKham phongKham) {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        return new PhongKhamTomTatResponse(phongKham.getId(), phongKham.ten(ngonNgu), phongKham.tang(ngonNgu));
    }

}
