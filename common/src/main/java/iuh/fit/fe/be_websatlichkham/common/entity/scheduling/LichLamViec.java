package iuh.fit.fe.be_websatlichkham.common.entity.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.CreatableEntity;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.PhongKham;
import iuh.fit.fe.be_websatlichkham.common.entity.identity.QuanTriVien;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLichLamViec;
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
 * Ca làm việc của bác sĩ tại 1 phòng khám trong 1 ngày (ERD: LichLamViec). Chỉ Admin xếp/sửa/xóa (SCHED-01/05/06).
 * <p>
 * Ca được chia thành đúng {@code soBenhNhanToiDa} {@link KhungGioKham}, mỗi khung nhận 1 bệnh nhân (Q3).
 * Không được chồng giờ theo bác sĩ hoặc theo phòng (kiểm ở LichLamViecService.assertNoOverlap).
 * <p>
 * Khác ERD: thêm {@code trangThai} để hủy ca khi Admin duyệt yêu cầu "Xin nghỉ" (SCHED-03)
 * mà vẫn giữ các khung giờ/lịch hẹn cũ để truy vết.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lich_lam_viec")
@AttributeOverride(name = "id", column = @Column(name = "id_lich_lam_viec"))
public class LichLamViec extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", nullable = false)
    private BacSi bacSi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_phong_kham", nullable = false)
    private PhongKham phongKham;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_admin_tao", nullable = false)
    private QuanTriVien adminTao;

    @Column(name = "ngay_lam_viec", nullable = false)
    private LocalDate ngayLamViec;

    @Column(name = "gio_bat_dau", nullable = false)
    private LocalTime gioBatDau;

    @Column(name = "gio_ket_thuc", nullable = false)
    private LocalTime gioKetThuc;

    @Column(name = "so_benh_nhan_toi_da", nullable = false)
    private Integer soBenhNhanToiDa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiLichLamViec trangThai = TrangThaiLichLamViec.HOAT_DONG;

}
