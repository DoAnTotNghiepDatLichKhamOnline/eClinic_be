package iuh.fit.se.eclinic.identity.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.identity.dto.response.PhienDangNhapResponse;

/** Chuyển dòng refresh_token còn hiệu lực -> DTO phiên đăng nhập. */
@Component
public class PhienDangNhapMapper {

    /** Phiên đăng nhập; {@code maPhienHienTai} null thì không phiên nào là "hiện tại". */
    public PhienDangNhapResponse toResponse(RefreshToken phien, String maPhienHienTai) {
        return new PhienDangNhapResponse(phien.getMaPhien(), phien.getThongTinThietBi(), phien.getNgayDangNhap(),
                phien.getNgayTao(), phien.getNgayHetHan(), phien.getMaPhien().equals(maPhienHienTai));
    }

}
