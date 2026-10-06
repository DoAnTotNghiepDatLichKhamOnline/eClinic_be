package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.AuditableEntity;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
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
 * Khác ERD: thêm {@code lyDoHuy}, {@code canDoiLich} (.md mục 15.4), {@code lichHenCu}, {@code nguoiGiamHo},
 * {@code taiKhoanDat}, {@code soDienThoaiLienHe}, các trường {@code *DaNhap} và {@code canDoiChieu}.
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

    /** Người giám hộ của lượt khám này, bắt buộc khi bệnh nhân dưới 18 tuổi (BOOK-11). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nguoi_giam_ho")
    private NguoiGiamHo nguoiGiamHo;

    /** Tài khoản đã đặt lịch; null = Khách đặt. Chỉ để biết ai đặt, lịch hẹn vẫn thuộc về hồ sơ bệnh nhân. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan_dat")
    private TaiKhoan taiKhoanDat;

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

    /**
     * SĐT người đặt nhập trên form, để liên hệ cho lượt khám này. Không ghi vào hồ sơ bệnh nhân: SĐT của hồ sơ là thứ
     * được đối chiếu khi liên kết hồ sơ vào tài khoản (quy tắc #3). Null với lịch hẹn tạo trước V5.
     */
    @Column(name = "so_dien_thoai_lien_he", length = 20)
    private String soDienThoaiLienHe;

    /** Email người đặt nhập trên form (không bắt buộc), để sau này gửi phiếu khám / nhắc lịch. */
    @Column(name = "email_lien_he", length = 255)
    private String emailLienHe;

    /**
     * Họ tên, ngày sinh, giới tính người khám và họ tên người giám hộ ĐÚNG như người đặt nhập trên form. Số CCCD đã có
     * hồ sơ thì lần đặt sau không sửa hồ sơ, nên phiếu khám và "lịch hẹn của tôi" hiện các trường này thay cho dữ liệu
     * của hồ sơ (không lộ dữ liệu người khác đã nhập). Null với lịch hẹn tạo trước V10.
     */
    @Column(name = "ho_ten_da_nhap", length = 150)
    private String hoTenDaNhap;

    @Column(name = "ngay_sinh_da_nhap")
    private LocalDate ngaySinhDaNhap;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gioi_tinh_da_nhap", length = 10)
    private GioiTinh gioiTinhDaNhap;

    /** Null khi lượt khám không có người giám hộ. */
    @Column(name = "ho_ten_giam_ho_da_nhap", length = 150)
    private String hoTenGiamHoDaNhap;

    /**
     * Họ tên / ngày sinh nhập vào khác hồ sơ bệnh nhân (hoặc khác người giám hộ đã khai): phòng khám đối chiếu giấy tờ
     * khi bệnh nhân đến khám rồi bỏ đánh dấu.
     */
    @Column(name = "can_doi_chieu", nullable = false)
    private boolean canDoiChieu;

    /**
     * Mã ngắn để đọc / gõ, dạng {@code ECL-<ngày khám yyyyMMdd>-<4 chữ số>}. Bác sĩ, quản trị viên tra lịch hẹn theo mã
     * này; KHÔNG dùng để mở phiếu khám công khai (đoán được).
     */
    @Column(name = "ma_tra_cuu", nullable = false, unique = true, length = 20)
    private String maTraCuu;

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
