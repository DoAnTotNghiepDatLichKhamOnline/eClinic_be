package iuh.fit.se.eclinic.catalog.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

/**
 * Chuyển entity bác sĩ sang DTO công khai. Họ tên, ảnh đại diện lấy từ tài khoản của bác sĩ.
 */
@Component
public class BacSiMapper {

    public BacSiResponse toResponse(BacSi bacSi) {
        return new BacSiResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(), bacSi.getTaiKhoan().getAnhDaiDien(),
                bacSi.getHocVi(), bacSi.getSoNamKinhNghiem(), bacSi.getChuyenKhoa().getId(),
                bacSi.getChuyenKhoa().getTenChuyenKhoa());
    }

    public BacSiChiTietResponse toChiTietResponse(BacSi bacSi) {
        return new BacSiChiTietResponse(bacSi.getId(), bacSi.getTaiKhoan().getHoTen(),
                bacSi.getTaiKhoan().getAnhDaiDien(), bacSi.getHocVi(), bacSi.getSoNamKinhNghiem(),
                bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa(), bacSi.getTieuSu());
    }

}
