package iuh.fit.se.eclinic.identity.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanResponse;

/**
 * Chuyển TaiKhoan -> DTO. Viết tay, không bao giờ đưa matKhauHash ra ngoài.
 */
@Component
public class TaiKhoanMapper {

    public TaiKhoanResponse toResponse(TaiKhoan taiKhoan) {
        return new TaiKhoanResponse(taiKhoan.getId(), taiKhoan.getHoTen(), taiKhoan.getEmail(),
                taiKhoan.getSoDienThoai(), taiKhoan.getVaiTro(), taiKhoan.getTrangThai());
    }

}
