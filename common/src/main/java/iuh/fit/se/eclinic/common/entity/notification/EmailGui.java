package iuh.fit.se.eclinic.common.entity.notification;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.HangChoGui;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiEmail;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Email chờ gửi của notification-service (hàng EMAIL_GUI của hang_cho_gui). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("EMAIL_GUI")
public class EmailGui extends HangChoGui {

    /** Mã sự kiện ở service nguồn (vd {@code booking:15}), UNIQUE: gửi lại cùng mã không tạo email thứ hai. */
    @Column(name = "ma_nguon", length = 64)
    private String maNguon;

    @Column(name = "email_nhan", length = 255)
    private String nguoiNhan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ngon_ngu", length = 5)
    private NgonNgu ngonNgu;

    /** Mẫu email (vd {@code email.datLich}); câu chữ trong thong-diep/notification*.properties. */
    @Column(name = "khoa_mau", length = 80)
    private String khoaMau;

    /** Tham số của mẫu, mảng JSON ({@code ThamSoThongBao}). */
    @Column(name = "tham_so", columnDefinition = "TEXT")
    private String thamSo;

    @Column(name = "id_lich_hen")
    private Long idLichHen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", length = 10)
    private TrangThaiEmail trangThai = TrangThaiEmail.CHO_GUI;

    /** Số lần gửi đã lỗi. */
    @Column(name = "so_lan_loi", nullable = false)
    private int soLanThu;

    /** Không gửi trước thời điểm này. */
    @Column(name = "lan_thu_ke_tiep")
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
