package iuh.fit.fe.be_websatlichkham.common.entity.identity;

import java.time.LocalDateTime;

import iuh.fit.fe.be_websatlichkham.common.entity.CreatableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một phiên đăng nhập (1 thiết bị) — ERD: RefreshToken.
 * <p>
 * Khác ERD: cột {@code token} lưu SHA-256 của refresh token ({@code token_hash}), không lưu token gốc;
 * thêm {@code ngayThuHoi} để thu hồi phiên (AUTH-03 đặt lại mật khẩu, ADM-03 vô hiệu hóa tài khoản).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_token")
@AttributeOverride(name = "id", column = @Column(name = "id_refresh_token"))
public class RefreshToken extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "thong_tin_thiet_bi")
    private String thongTinThietBi;

    @Column(name = "ngay_het_han", nullable = false)
    private LocalDateTime ngayHetHan;

    /** NULL = còn hiệu lực. */
    @Column(name = "ngay_thu_hoi")
    private LocalDateTime ngayThuHoi;

    public boolean conHieuLuc(LocalDateTime now) {
        return ngayThuHoi == null && ngayHetHan.isAfter(now);
    }

}
