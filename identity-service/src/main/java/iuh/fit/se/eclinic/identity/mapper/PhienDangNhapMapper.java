package iuh.fit.se.eclinic.identity.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.identity.dto.response.PhienDangNhapResponse;

/**
 * Chuyển dòng refresh_token còn hiệu lực -> DTO phiên đăng nhập. Viết tay, không bao giờ đưa tokenHash ra ngoài.
 */
@Component
public class PhienDangNhapMapper {

    /** @param maPhienHienTai mã phiên của access token đang gọi API; null thì không phiên nào là "hiện tại" */
    public PhienDangNhapResponse toResponse(RefreshToken phien, String maPhienHienTai) {
        return new PhienDangNhapResponse(phien.getMaPhien(), phien.getThongTinThietBi(), phien.getNgayDangNhap(),
                phien.getNgayTao(), phien.getNgayHetHan(), phien.getMaPhien().equals(maPhienHienTai));
    }

}
