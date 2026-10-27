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

/** Một phiên đăng nhập (1 thiết bị). */
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
