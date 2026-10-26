package iuh.fit.se.eclinic.common.entity.booking;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Quản trị viên đã từ chối gắn 1 hồ sơ bệnh nhân vào 1 tài khoản (quy tắc #3). Có dòng này thì hệ thống không tự đưa
 * cặp (tài khoản, hồ sơ) đó vào hàng chờ xác minh nữa; người dùng phải liên hệ phòng khám.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lien_ket_ho_so_bi_tu_choi", uniqueConstraints = @UniqueConstraint(
        name = "uk_lien_ket_bi_tu_choi_tai_khoan_ho_so", columnNames = { "id_tai_khoan", "id_ho_so_benh_nhan" }))
@AttributeOverride(name = "id", column = @Column(name = "id_lien_ket_bi_tu_choi"))
public class LienKetHoSoBiTuChoi extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ho_so_benh_nhan", nullable = false)
    private HoSoBenhNhan hoSoBenhNhan;

    @Column(name = "ly_do", length = 500)
    private String lyDo;

}
