package iuh.fit.se.eclinic.common.entity.medical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.util.DanhMucIcd10;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Hồ sơ bệnh án của 1 lượt khám: 1-1 với lịch hẹn, kèm đơn thuốc. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ho_so_benh_an")
@AttributeOverride(name = "id", column = @Column(name = "id_ho_so_benh_an"))
public class HoSoBenhAn extends CreatableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lich_hen", nullable = false, unique = true)
    private LichHen lichHen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ho_so_benh_nhan", nullable = false)
    private HoSoBenhNhan hoSoBenhNhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", nullable = false)
    private BacSi bacSi;

    /** Lý do khám, triệu chứng; màn hình khám điền sẵn lý do bệnh nhân ghi khi đặt lịch. */
    @Column(name = "ly_do_kham", columnDefinition = "TEXT")
    private String lyDoKham;

    // Sinh hiệu: mạch, nhịp thở (lần/phút), nhiệt độ (°C), huyết áp (mmHg), cân nặng (kg), chiều cao (cm)
    @Column(name = "mach")
    private Integer mach;

    @Column(name = "nhiet_do", precision = 3, scale = 1)
    private BigDecimal nhietDo;

    @Column(name = "huyet_ap_tam_thu")
    private Integer huyetApTamThu;

    @Column(name = "huyet_ap_tam_truong")
    private Integer huyetApTamTruong;

    @Column(name = "nhip_tho")
    private Integer nhipTho;

    /** Bắt buộc khi kê thuốc cho trẻ dưới 72 tháng (Thông tư 26/2025/TT-BYT, kiểm tra trong medical-service). */
    @Column(name = "can_nang", precision = 5, scale = 2)
    private BigDecimal canNang;

    @Column(name = "chieu_cao", precision = 4, scale = 1)
    private BigDecimal chieuCao;

    /** Kết quả khám lâm sàng. */
    @Column(name = "kham_lam_sang", columnDefinition = "TEXT")
    private String khamLamSang;

    @Column(name = "chan_doan", columnDefinition = "TEXT")
    private String chanDoan;

    /**
     * Mã ICD-10 của chẩn đoán chính (không bắt buộc); mã mạn tính cho kê thuốc tối đa 90 ngày thay vì 30. Danh mục nằm
     * trong tệp ({@link DanhMucIcd10}), không phải bảng: đọc / ghi qua {@link #getIcdChinh()} / {@link #setIcdChinh}.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "ma_icd_chinh", length = 10)
    private String maIcdChinh;

    /** Các bệnh kèm theo, chữ tự do. */
    @Column(name = "chan_doan_kem_theo", columnDefinition = "TEXT")
    private String chanDoanKemTheo;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    /** Gợi ý ngày tái khám. KHÔNG tự tạo lịch hẹn. */
    @Column(name = "ngay_tai_kham_de_xuat")
    private LocalDate ngayTaiKhamDeXuat;

    /**
     * Ngày tái khám đã báo cho bệnh nhân; null = chưa báo, hoặc bác sĩ đã xoá ngày. Chỉ đọc: chỉ job nhắc tái khám của
     * booking-service ghi, bằng câu UPDATE riêng ({@code NhacTaiKhamRepository}), nên lần lưu hồ sơ của bác sĩ không ghi đè.
     */
    @Column(name = "tai_kham_ngay_da_bao", insertable = false, updatable = false)
    private LocalDate taiKhamNgayDaBao;

    /** Lần nhắc trước {@link #taiKhamNgayDaBao} đã được xử lý: đã nhắc, hoặc bỏ qua. Chỉ đọc, như cột trên. */
    @Column(name = "tai_kham_da_nhac_truoc", insertable = false, updatable = false)
    private boolean taiKhamDaNhacTruoc;

    @Column(name = "ma_token_so_kham", nullable = false, unique = true, length = 64)
    private String maTokenSoKham;

    /** 1 khi ghi nhận, tăng 1 mỗi lần sửa; client gửi lại khi sửa để không đè lên lần sửa của người khác. */
    @Column(name = "phien_ban", nullable = false)
    private int phienBan = 1;

    /** Lần sửa gần nhất; null = chưa sửa lần nào. */
    @Column(name = "ngay_sua_cuoi")
    private LocalDateTime ngaySuaCuoi;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "hoSoBenhAn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietDonThuoc> chiTietDonThuoc = new ArrayList<>();

    /** null nếu chưa chọn mã (hoặc mã cũ đã bị bỏ khỏi tệp danh mục). */
    public Icd10 getIcdChinh() {
        return maIcdChinh == null ? null : DanhMucIcd10.tim(maIcdChinh).orElse(null);
    }

    public void setIcdChinh(Icd10 icdChinh) {
        this.maIcdChinh = icdChinh == null ? null : icdChinh.getMa();
    }

    public void addChiTietDonThuoc(ChiTietDonThuoc chiTiet) {
        chiTietDonThuoc.add(chiTiet);
        chiTiet.setHoSoBenhAn(this);
    }

    public void removeChiTietDonThuoc(ChiTietDonThuoc chiTiet) {
        chiTietDonThuoc.remove(chiTiet);
        chiTiet.setHoSoBenhAn(null);
    }

}
