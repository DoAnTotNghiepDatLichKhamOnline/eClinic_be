package iuh.fit.fe.be_websatlichkham.common.entity.identity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.AuditableEntity;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiTaiKhoan;
import iuh.fit.fe.be_websatlichkham.common.enums.VaiTro;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tài khoản đăng nhập (ERD: TaiKhoan). Thông tin theo vai trò nằm ở {@link QuanTriVien}, BacSi, HoSoBenhNhan.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tai_khoan")
@AttributeOverride(name = "id", column = @Column(name = "id_tai_khoan"))
public class TaiKhoan extends AuditableEntity {

    @Column(name = "ho_ten", length = 150)
    private String hoTen;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "mat_khau_hash", nullable = false)
    private String matKhauHash;

    /** Duy nhất trên toàn hệ thống (AUTH-04). */
    @Column(name = "so_dien_thoai", length = 20, unique = true)
    private String soDienThoai;

    @Column(name = "anh_dai_dien", length = 500)
    private String anhDaiDien;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "vai_tro", nullable = false, length = 20)
    private VaiTro vaiTro;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiTaiKhoan trangThai = TrangThaiTaiKhoan.CHO_XAC_NHAN;

    /** Bổ sung ngoài ERD: lý do Admin vô hiệu hóa tài khoản (ADM-03 bắt buộc chọn lý do). */
    @Column(name = "ly_do_vo_hieu_hoa", length = 500)
    private String lyDoVoHieuHoa;

}
