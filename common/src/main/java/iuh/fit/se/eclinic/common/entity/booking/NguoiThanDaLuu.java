package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Người thân mà 1 tài khoản đã đặt lịch cho, để điền sẵn form đặt lịch lần sau.
 * <p>
 * Lưu ĐÚNG những gì tài khoản đó nhập trên form lần đặt gần nhất, không đọc lại từ {@link HoSoBenhNhan}: hồ sơ gom dữ
 * liệu của nhiều người đặt khác nhau, trả hồ sơ cho tài khoản sẽ lộ dữ liệu người khác nhập. {@code hoSoBenhNhan} chỉ
 * là khoá để biết lần đặt sau là cùng 1 người. Các trường {@code giamHo*} null khi lần đặt gần nhất không có người
 * giám hộ.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "nguoi_than_da_luu", uniqueConstraints = @UniqueConstraint(name = "uk_nguoi_than_da_luu_tai_khoan_ho_so",
        columnNames = { "id_tai_khoan", "id_ho_so_benh_nhan" }))
@AttributeOverride(name = "id", column = @Column(name = "id_nguoi_than_da_luu"))
public class NguoiThanDaLuu extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ho_so_benh_nhan", nullable = false)
    private HoSoBenhNhan hoSoBenhNhan;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String hoTen;

    @Column(name = "ngay_sinh", nullable = false)
    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gioi_tinh", length = 10)
    private GioiTinh gioiTinh;

    /** null = người dưới 18 tuổi chưa có CCCD. */
    @Column(name = "cccd", length = 12)
    private String cccd;

    @Column(name = "so_dien_thoai", nullable = false, length = 20)
    private String soDienThoai;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "dia_chi", length = 500)
    private String diaChi;

    @Column(name = "so_bao_hiem_y_te", length = 50)
    private String soBaoHiemYTe;

    @Column(name = "giam_ho_ho_ten", length = 150)
    private String giamHoHoTen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "giam_ho_quan_he", length = 30)
    private QuanHeGiamHo giamHoQuanHe;

    @Column(name = "giam_ho_so_dien_thoai", length = 20)
    private String giamHoSoDienThoai;

    @Column(name = "giam_ho_cccd", length = 12)
    private String giamHoCccd;

    @Column(name = "giam_ho_ngay_sinh")
    private LocalDate giamHoNgaySinh;

    /** Lần đặt lịch gần nhất cho người này. */
    @Column(name = "lan_dung_cuoi", nullable = false)
    private LocalDateTime lanDungCuoi;

}
