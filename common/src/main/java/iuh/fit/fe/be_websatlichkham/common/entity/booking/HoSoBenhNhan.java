package iuh.fit.fe.be_websatlichkham.common.entity.booking;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.CreatableEntity;
import iuh.fit.fe.be_websatlichkham.common.entity.identity.TaiKhoan;
import iuh.fit.fe.be_websatlichkham.common.enums.GioiTinh;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLienKet;
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
 * Khác ERD: thêm {@code tienSuBenhLy} (PAT-01) và {@code trangThaiLienKet} (Q2).
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

    @Column(name = "cccd", nullable = false, unique = true, length = 12)
    private String cccd;

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
