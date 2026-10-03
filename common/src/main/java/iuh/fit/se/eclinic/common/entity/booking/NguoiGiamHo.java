package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
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
 * Người giám hộ khai khi đặt lịch cho bệnh nhân dưới 18 tuổi (BOOK-11, quy tắc #9).
 * <p>
 * Thuộc về 1 hồ sơ bệnh nhân; cùng CCCD chỉ lưu 1 lần cho 1 hồ sơ nên các lần đặt sau dùng lại.
 * {@link LichHen#getNguoiGiamHo()} trỏ tới người giám hộ của lượt khám đó.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "nguoi_giam_ho", uniqueConstraints = @UniqueConstraint(name = "uk_nguoi_giam_ho_ho_so_cccd",
        columnNames = { "id_ho_so_benh_nhan", "cccd" }))
@AttributeOverride(name = "id", column = @Column(name = "id_nguoi_giam_ho"))
public class NguoiGiamHo extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ho_so_benh_nhan", nullable = false)
    private HoSoBenhNhan hoSoBenhNhan;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String hoTen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "quan_he", nullable = false, length = 30)
    private QuanHeGiamHo quanHe;

    @Column(name = "so_dien_thoai", nullable = false, length = 20)
    private String soDienThoai;

    @Column(name = "cccd", nullable = false, length = 12)
    private String cccd;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

}
