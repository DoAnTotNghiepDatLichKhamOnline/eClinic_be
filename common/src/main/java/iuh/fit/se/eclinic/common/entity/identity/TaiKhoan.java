package iuh.fit.se.eclinic.common.entity.identity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.AuditableEntity;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tài khoản đăng nhập. */
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

    /** NULL: tài khoản chỉ đăng nhập bằng Google (đăng nhập mật khẩu luôn báo sai thông tin). */
    @Column(name = "mat_khau_hash")
    private String matKhauHash;

    /** Bác sĩ đang mang mật khẩu mặc định: chưa được cấp phiên cho tới khi tự đặt mật khẩu. */
    @Column(name = "phai_doi_mat_khau", nullable = false)
    private boolean phaiDoiMatKhau;

    /** "sub" trong ID token Google: định danh bất biến của tài khoản Google đã liên kết. */
    @Column(name = "google_id", unique = true)
    private String googleId;

    /** Duy nhất trên toàn hệ thống. */
    @Column(name = "so_dien_thoai", length = 20, unique = true)
    private String soDienThoai;

    /** Số CCCD khai khi đăng ký (chưa kiểm chứng); booking-service dùng để liên kết hồ sơ bệnh nhân. */
    @Column(name = "cccd_dang_ky", length = 12)
    private String cccdDangKy;

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

    /** Ngôn ngữ ưa dùng. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ngon_ngu", nullable = false, length = 5)
    private NgonNgu ngonNgu = NgonNgu.VI;

    /** Lý do quản trị viên vô hiệu hóa tài khoản. */
    @Column(name = "ly_do_vo_hieu_hoa", length = 500)
    private String lyDoVoHieuHoa;

}
