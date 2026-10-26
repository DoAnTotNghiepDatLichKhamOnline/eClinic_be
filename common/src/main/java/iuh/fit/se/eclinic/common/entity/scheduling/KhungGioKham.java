package iuh.fit.se.eclinic.common.entity.scheduling;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.BaseEntity;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Khung giờ khám (ERD: KhungGioKham), sinh tự động khi Admin tạo/sửa ca.
 * <p>
 * Mỗi dòng là 1 lượt khám (1 bệnh nhân) dài {@code thoiLuongLuotPhut} phút của ca. Khung 1 giờ của BOOK-12 không lưu
 * thành dòng: đó là các lượt có giờ bắt đầu nằm trong cùng 1 giờ tính từ giờ bắt đầu ca (xem {@link LichLamViec}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "khung_gio_kham", uniqueConstraints = @UniqueConstraint(name = "uk_khung_gio_kham_ca_gio_bat_dau",
        columnNames = { "id_lich_lam_viec", "gio_bat_dau" }))
@AttributeOverride(name = "id", column = @Column(name = "id_khung_gio"))
public class KhungGioKham extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lich_lam_viec", nullable = false)
    private LichLamViec lichLamViec;

    @Column(name = "gio_bat_dau", nullable = false)
    private LocalDateTime gioBatDau;

    @Column(name = "gio_ket_thuc", nullable = false)
    private LocalDateTime gioKetThuc;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiKhungGio trangThai = TrangThaiKhungGio.CON_TRONG;

    /** Optimistic locking khi nhiều người cùng đặt 1 khung giờ. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
