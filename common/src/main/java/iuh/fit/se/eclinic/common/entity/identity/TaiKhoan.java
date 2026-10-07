package iuh.fit.se.eclinic.common.entity.identity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.AuditableEntity;
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

    /** NULL: tài khoản chỉ đăng nhập bằng Google (đăng nhập mật khẩu luôn báo sai thông tin). */
    @Column(name = "mat_khau_hash")
    private String matKhauHash;

    /**
     * Tài khoản bác sĩ do quản trị viên tạo đang mang mật khẩu mặc định (V13): đăng nhập đúng mật khẩu vẫn không được
     * cấp phiên cho tới khi chủ tài khoản đặt mật khẩu của mình (POST /api/auth/first-password hoặc liên kết đặt lại).
     */
    @Column(name = "phai_doi_mat_khau", nullable = false)
    private boolean phaiDoiMatKhau;

    /** "sub" trong ID token Google: định danh bất biến của tài khoản Google đã liên kết (V2). */
    @Column(name = "google_id", unique = true)
    private String googleId;

    /** Duy nhất trên toàn hệ thống (AUTH-04). */
    @Column(name = "so_dien_thoai", length = 20, unique = true)
    private String soDienThoai;

    /**
     * Số CCCD khai khi đăng ký (không bắt buộc, chưa được kiểm chứng). booking-service dùng để tìm hồ sơ bệnh nhân đã
     * có của Khách và liên kết vào tài khoản (quy tắc #3). Không duy nhất, không trả về trong API nào.
     */
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

    /** Bổ sung ngoài ERD: lý do Admin vô hiệu hóa tài khoản (ADM-03 bắt buộc chọn lý do). */
    @Column(name = "ly_do_vo_hieu_hoa", length = 500)
    private String lyDoVoHieuHoa;

}
