package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hồ sơ bệnh nhân (ERD: HoSoBenhNhan), định danh bằng CCCD (quy tắc #1, BOOK-03).
 * <ul>
 *   <li>Khách đặt lịch: tạo hồ sơ theo CCCD, {@code taiKhoan == null} ({@link TrangThaiLienKet#CHUA_LIEN_KET}).</li>
 *   <li>1 tài khoản gắn tối đa 1 hồ sơ (quy tắc #13). Liên kết hồ sơ cũ khi kích hoạt tài khoản phải qua xác minh
 *       (quy tắc #3 — Q2): SĐT khớp -> DA_LIEN_KET, không khớp -> CHO_XAC_MINH chờ Admin.</li>
 * </ul>
 * Bệnh nhân dưới 18 tuổi chưa có CCCD (quy tắc #10): {@code cccd == null}, hồ sơ nhận diện bằng {@code khoaNhanDien}.
 * <p>
 * Khác ERD: thêm {@code tienSuBenhLy} (PAT-01), {@code trangThaiLienKet} (Q2) và {@code khoaNhanDien}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ho_so_benh_nhan")
@AttributeOverride(name = "id", column = @Column(name = "id_ho_so_benh_nhan"))
public class HoSoBenhNhan extends CreatableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan", unique = true)
    private TaiKhoan taiKhoan;

    /** null chỉ với bệnh nhân dưới 18 tuổi chưa có CCCD (quy tắc #10): khi đó phải có {@link #khoaNhanDien}. */
    @Column(name = "cccd", unique = true, length = 12)
    private String cccd;

    /**
     * Khoá nhận diện của hồ sơ tạo khi chưa có CCCD: SHA-256 của (họ tên đã chuẩn hoá | ngày sinh | CCCD người giám
     * hộ). Giữ nguyên sau khi hồ sơ được điền CCCD.
     */
    @Column(name = "khoa_nhan_dien", unique = true, length = 64)
    private String khoaNhanDien;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String hoTen;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gioi_tinh", length = 10)
    private GioiTinh gioiTinh;

    @Column(name = "so_dien_thoai", nullable = false, length = 20)
    private String soDienThoai;

    @Column(name = "dia_chi", length = 500)
    private String diaChi;

    @Column(name = "so_bao_hiem_y_te", length = 50)
    private String soBaoHiemYTe;

    @Column(name = "tien_su_benh_ly", columnDefinition = "TEXT")
    private String tienSuBenhLy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai_lien_ket", nullable = false, length = 20)
    private TrangThaiLienKet trangThaiLienKet = TrangThaiLienKet.CHUA_LIEN_KET;

}
