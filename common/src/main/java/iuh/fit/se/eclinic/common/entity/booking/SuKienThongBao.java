package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
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
 * Thông báo chờ gửi sang notification-service (không có trong ERD; bảng "outbox" của booking-service, V11). Ghi trong
 * cùng transaction với việc đổi trạng thái lịch hẹn, nên có lịch hẹn đổi trạng thái là có dòng này; việc gửi làm sau.
 * <p>
 * {@code idTaiKhoan}, {@code idLichHen} là số thường, không phải quan hệ: bảng không có khoá ngoại tới miền khác.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "su_kien_thong_bao")
@AttributeOverride(name = "id", column = @Column(name = "id_su_kien"))
public class SuKienThongBao extends CreatableEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loai", nullable = false, length = 30)
    private LoaiThongBao loai;

    /** Tài khoản nhận thông báo. */
    @Column(name = "id_tai_khoan", nullable = false)
    private Long idTaiKhoan;

    @Column(name = "id_lich_hen")
    private Long idLichHen;

    /** Yêu cầu đổi ca / xin nghỉ liên quan (V12). */
    @Column(name = "id_yeu_cau")
    private Long idYeuCau;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    /** Số lần notification-service từ chối riêng dòng này (dữ liệu sai); mất kết nối không tính. */
    @Column(name = "so_lan_loi", nullable = false)
    private int soLanLoi;

    /** Null = chưa gửi được. */
    @Column(name = "ngay_gui")
    private LocalDateTime ngayGui;

}
