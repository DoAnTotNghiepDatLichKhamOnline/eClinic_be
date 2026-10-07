package iuh.fit.se.eclinic.common.entity.notification;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thông báo trong ứng dụng (ERD: ThongBao — NOTI-01). Chỉ gửi cho tài khoản đăng nhập, Khách không nhận.
 * <p>
 * Khác ERD: thêm {@code yeuCauDoiLich} để bấm vào thông báo chuyển tới yêu cầu đổi lịch (Admin/Bác sĩ), và
 * {@code maNguon} (V11).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "thong_bao")
@AttributeOverride(name = "id", column = @Column(name = "id_thong_bao"))
public class ThongBao extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lich_hen")
    private LichHen lichHen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_yeu_cau")
    private YeuCauDoiLich yeuCauDoiLich;

    /**
     * Mã của sự kiện đã sinh ra thông báo (vd {@code booking:15}), UNIQUE: service nguồn gửi lại cùng 1 sự kiện thì không
     * tạo thông báo thứ hai. Null với thông báo không sinh từ sự kiện.
     */
    @Column(name = "ma_nguon", length = 64)
    private String maNguon;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loai", nullable = false, length = 30)
    private LoaiThongBao loai;

    @Column(name = "da_doc", nullable = false)
    private boolean daDoc;

}
