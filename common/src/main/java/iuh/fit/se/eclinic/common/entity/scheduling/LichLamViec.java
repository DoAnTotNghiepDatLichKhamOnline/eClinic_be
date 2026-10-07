package iuh.fit.se.eclinic.common.entity.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
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
 * Sức chứa (BOOK-12): ca gồm các khung 1 giờ tính từ giờ bắt đầu ca, mỗi khung nhận tối đa
 * {@code soLuotToiDaMoiGio} (N) lượt, mỗi lượt {@code thoiLuongLuotPhut} (t) phút, N x t &lt;= 60.
 * Mỗi lượt là 1 dòng {@link KhungGioKham}; {@code soBenhNhanToiDa} là tổng số lượt của cả ca.
 * Không được chồng giờ theo bác sĩ hoặc theo phòng (kiểm ở LichLamViecService.kiemTraKhongTrungLich).
 * <p>
 * Khác ERD: thêm {@code trangThai} để hủy ca (Admin hủy trực tiếp, hoặc duyệt yêu cầu xin nghỉ / đổi ca, SCHED-03/06)
 * mà vẫn giữ các khung giờ / lịch hẹn cũ để truy vết: ca không bao giờ bị xoá. Lịch hẹn còn hiệu lực của ca bị hủy được
 * đánh dấu {@code LichHen.canDoiLich} (xem QuanLyCaService của booking-service).
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

    @Column(name = "so_luot_toi_da_moi_gio", nullable = false)
    private Integer soLuotToiDaMoiGio;

    @Column(name = "thoi_luong_luot_phut", nullable = false)
    private Integer thoiLuongLuotPhut;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiLichLamViec trangThai = TrangThaiLichLamViec.HOAT_DONG;

}
