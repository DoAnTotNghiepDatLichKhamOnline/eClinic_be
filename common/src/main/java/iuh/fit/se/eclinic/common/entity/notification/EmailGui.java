package iuh.fit.se.eclinic.common.entity.notification;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiEmail;
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
 * Email chờ gửi của notification-service (không có trong ERD; V19). Service khác gửi sang 1 mẫu + tham số, ở đây giữ lại
 * cho tới khi máy chủ mail nhận: gửi lỗi thì thử lại sau, không mất email khi máy chủ mail tạm ngừng.
 * <p>
 * Nội dung không lưu sẵn: soạn lúc gửi từ {@code khoaMau} + {@code thamSo} theo {@code ngonNgu}. {@code idLichHen} là số
 * thường, không phải quan hệ: bảng không có khoá ngoại tới miền khác.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "email_gui")
@AttributeOverride(name = "id", column = @Column(name = "id_email"))
public class EmailGui extends CreatableEntity {

    /** Mã của sự kiện ở service nguồn (vd {@code booking:15}), UNIQUE: gửi lại cùng mã thì không có email thứ hai. */
    @Column(name = "ma_nguon", nullable = false, length = 64)
    private String maNguon;

    @Column(name = "nguoi_nhan", nullable = false, length = 255)
    private String nguoiNhan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ngon_ngu", nullable = false, length = 5)
    private NgonNgu ngonNgu;

    /** Mẫu email (vd {@code email.datLich}); câu chữ trong thong-diep/notification*.properties. */
    @Column(name = "khoa_mau", nullable = false, length = 80)
    private String khoaMau;

    /** Tham số của mẫu, mảng JSON (xem {@code ThamSoThongBao}). */
    @Column(name = "tham_so", columnDefinition = "TEXT")
    private String thamSo;

    @Column(name = "id_lich_hen")
    private Long idLichHen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 10)
    private TrangThaiEmail trangThai = TrangThaiEmail.CHO_GUI;

    /** Số lần gửi đã lỗi. */
    @Column(name = "so_lan_thu", nullable = false)
    private int soLanThu;

    /** Không gửi trước thời điểm này. */
    @Column(name = "lan_thu_ke_tiep", nullable = false)
    private LocalDateTime lanThuKeTiep;

    /** Tên loại lỗi của lần gửi gần nhất; không chứa địa chỉ hay nội dung email. */
    @Column(name = "loi_cuoi", length = 100)
    private String loiCuoi;

    @Column(name = "ngay_gui")
    private LocalDateTime ngayGui;

    /** Nội dung tệp .ics gửi kèm thành lich-kham.ics; null = không đính kèm. */
    @Column(name = "tep_lich", columnDefinition = "TEXT")
    private String tepLich;

}
