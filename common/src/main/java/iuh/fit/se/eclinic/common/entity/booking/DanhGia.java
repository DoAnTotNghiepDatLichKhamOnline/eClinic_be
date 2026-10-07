package iuh.fit.se.eclinic.common.entity.booking;

import iuh.fit.se.eclinic.common.entity.AuditableEntity;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Đánh giá của bệnh nhân cho 1 lượt khám đã hoàn thành: mỗi lịch hẹn tối đa 1 đánh giá.
 * <p>
 * {@code bacSi} lặp lại từ lịch hẹn để tính điểm trung bình của bác sĩ không phải join. Nhận xét chỉ bác sĩ và quản trị
 * viên đọc; công khai chỉ có điểm trung bình và số lượt đánh giá.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "danh_gia")
@AttributeOverride(name = "id", column = @Column(name = "id_danh_gia"))
public class DanhGia extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lich_hen", nullable = false, unique = true)
    private LichHen lichHen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", nullable = false)
    private BacSi bacSi;

    /** Tài khoản bệnh nhân đã gửi đánh giá. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    /** 1..5. */
    @Column(name = "so_sao", nullable = false)
    private int soSao;

    @Column(name = "nhan_xet", length = 1000)
    private String nhanXet;
}
