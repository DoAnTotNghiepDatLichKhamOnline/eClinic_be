package iuh.fit.fe.be_websatlichkham.common.entity.booking;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.AuditableEntity;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.PhongKham;
import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.KhungGioKham;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLichHen;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lịch hẹn khám (ERD: LichHen). Gắn với hồ sơ bệnh nhân (CCCD), không gắn trực tiếp tài khoản (quy tắc #2).
 * <p>
 * Chống đặt trùng ở mức DB: cột generated {@code id_khung_gio_hieu_luc} (không map trong entity) + UNIQUE,
 * nên 2 lịch hẹn có {@link TrangThaiLichHen#chiemKhungGio()} trên cùng 1 khung giờ sẽ ném
 * {@code DataIntegrityViolationException}.
 * <p>
 * Đổi lịch = hủy + tạo mới (quy tắc #10): lịch cũ -> DA_HUY_DO_DOI_LICH, lịch mới có {@code lichHenCu} trỏ về lịch cũ.
 * <p>
 * Khác ERD: thêm {@code lyDoHuy}, {@code canDoiLich} (.md mục 15.4) và {@code lichHenCu}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lich_hen")
@AttributeOverride(name = "id", column = @Column(name = "id_lich_hen"))
public class LichHen extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ho_so_benh_nhan", nullable = false)
    private HoSoBenhNhan hoSoBenhNhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", nullable = false)
    private BacSi bacSi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_khung_gio", nullable = false)
    private KhungGioKham khungGio;

    /** Denormalized từ khungGio -> lichLamViec -> phongKham (dùng để sinh số thứ tự theo phòng + ngày). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_phong_kham", nullable = false)
    private PhongKham phongKham;

    /** Số thứ tự khám theo phòng khám + ngày (quy tắc #4). */
    @Column(name = "so_thu_tu", nullable = false)
    private Integer soThuTu;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 30)
    private TrangThaiLichHen trangThai = TrangThaiLichHen.CHO_XAC_NHAN;

    @Column(name = "ly_do_kham", length = 500)
    private String lyDoKham;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    /** Chuỗi ngẫu nhiên cho link/QR phiếu khám (quy tắc #8), không dùng id tuần tự. */
    @Column(name = "ma_token_phieu_kham", nullable = false, unique = true, length = 64)
    private String maTokenPhieuKham;

    /** Lý do hủy (BOOK-09, tùy chọn) hoặc lý do từ chối (BOOK-10, bắt buộc). */
    @Column(name = "ly_do_huy", length = 500)
    private String lyDoHuy;

    /** Đánh dấu "cần dời/hủy" khi ca làm việc bị đổi/hủy (SCHED-03, SCHED-05). */
    @Column(name = "can_doi_lich", nullable = false)
    private boolean canDoiLich;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lich_hen_cu")
    private LichHen lichHenCu;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
