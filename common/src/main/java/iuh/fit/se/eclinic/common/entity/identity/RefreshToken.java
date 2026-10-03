package iuh.fit.se.eclinic.common.entity.identity;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
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
 * <p>
 * Mỗi lần làm mới phiên tạo 1 dòng mới (token xoay vòng) và thu hồi dòng cũ; các dòng của cùng 1 lần đăng nhập
 * có chung {@code maPhien} và {@code ngayDangNhap} (V3). Một phiên có nhiều nhất 1 dòng còn hiệu lực.
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

    /** Mã phiên đăng nhập: UUID cấp khi đăng nhập, giữ nguyên qua các lần làm mới; có trong access token. */
    @Column(name = "ma_phien", nullable = false, length = 36)
    private String maPhien;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "thong_tin_thiet_bi")
    private String thongTinThietBi;

    @Column(name = "ngay_het_han", nullable = false)
    private LocalDateTime ngayHetHan;

    /** NULL = còn hiệu lực. */
    @Column(name = "ngay_thu_hoi")
    private LocalDateTime ngayThuHoi;

    /** Thời điểm đăng nhập ban đầu của phiên ({@code ngayTao} là lần làm mới gần nhất). */
    @Column(name = "ngay_dang_nhap", nullable = false)
    private LocalDateTime ngayDangNhap;

    public boolean conHieuLuc(LocalDateTime now) {
        return ngayThuHoi == null && ngayHetHan.isAfter(now);
    }

}
