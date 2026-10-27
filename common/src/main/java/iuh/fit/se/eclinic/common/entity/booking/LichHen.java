package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.AuditableEntity;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiHangDoi;
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

/** Lịch hẹn khám, gắn với hồ sơ bệnh nhân; đổi lịch = hủy lịch cũ + tạo lịch mới. */
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

    /** Người giám hộ của lượt khám này, bắt buộc khi bệnh nhân dưới 18 tuổi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nguoi_giam_ho")
    private NguoiGiamHo nguoiGiamHo;

    /** Tài khoản đã đặt lịch; null = Khách đặt. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan_dat")
    private TaiKhoan taiKhoanDat;

    /** Kênh đặt lịch; đổi lịch giữ kênh của lịch cũ. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "kenh_dat", nullable = false, length = 20)
    private KenhDatLich kenhDat = KenhDatLich.KHACH;

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

    /** Số thứ tự khám theo phòng khám + ngày. */
    @Column(name = "so_thu_tu", nullable = false)
    private Integer soThuTu;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 30)
    private TrangThaiLichHen trangThai = TrangThaiLichHen.DA_XAC_NHAN;

    @Column(name = "ly_do_kham", length = 500)
    private String lyDoKham;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    /** SĐT người đặt nhập trên form, để liên hệ cho lượt khám này. */
    @Column(name = "so_dien_thoai_lien_he", length = 20)
    private String soDienThoaiLienHe;

    /** Email người đặt nhập trên form (không bắt buộc). */
    @Column(name = "email_lien_he", length = 255)
    private String emailLienHe;

    /** Ngôn ngữ của request đã tạo lịch hẹn. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ngon_ngu", length = 5)
    private NgonNgu ngonNgu;

    /** Họ tên, ngày sinh, giới tính người khám và họ tên người giám hộ ĐÚNG như người đặt nhập trên form. */
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

    /** Thông tin nhập khác hồ sơ bệnh nhân: phòng khám đối chiếu giấy tờ khi bệnh nhân đến. */
    @Column(name = "can_doi_chieu", nullable = false)
    private boolean canDoiChieu;

    /** Mã ngắn để đọc / gõ, dạng {@code ECL-<ngày khám yyyyMMdd>-<4 chữ số>}. */
    @Column(name = "ma_tra_cuu", nullable = false, unique = true, length = 20)
    private String maTraCuu;

    /** Chuỗi ngẫu nhiên cho link/QR phiếu khám, không dùng id tuần tự. */
    @Column(name = "ma_token_phieu_kham", nullable = false, unique = true, length = 64)
    private String maTokenPhieuKham;

    /** Lý do hủy (tùy chọn) hoặc lý do từ chối (bắt buộc). */
    @Column(name = "ly_do_huy", length = 500)
    private String lyDoHuy;

    /** Ca khám đã bị hủy: lịch hẹn giữ trạng thái, bệnh nhân phải đổi khung giờ khác hoặc hủy. */
    @Column(name = "can_doi_lich", nullable = false)
    private boolean canDoiLich;

    /** Ca được nhường cho bác sĩ khác: bệnh nhân được hủy / đổi tới giờ khám, không tính lượt đổi. */
    @Column(name = "doi_bac_si", nullable = false)
    private boolean doiBacSi;

    /** Trạng thái trong hàng đợi khám của bác sĩ (chỉ booking-service ghi); không bao giờ lưu DA_KHAM. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai_hang_doi", nullable = false, length = 20)
    private TrangThaiHangDoi trangThaiHangDoi = TrangThaiHangDoi.CHO_KHAM;

    /** Lần gần nhất bác sĩ gọi bệnh nhân vào khám. */
    @Column(name = "goi_luc")
    private LocalDateTime goiLuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lich_hen_cu")
    private LichHen lichHenCu;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Trạng thái hàng đợi để hiển thị: DA_KHAM khi đã có kết quả, VANG_MAT khi không đến, còn lại giá trị đã lưu. */
    public TrangThaiHangDoi hangDoiHienThi() {
        return switch (trangThai) {
            case DA_HOAN_THANH -> TrangThaiHangDoi.DA_KHAM;
            case KHONG_DEN -> TrangThaiHangDoi.VANG_MAT;
            default -> trangThaiHangDoi;
        };
    }

}
