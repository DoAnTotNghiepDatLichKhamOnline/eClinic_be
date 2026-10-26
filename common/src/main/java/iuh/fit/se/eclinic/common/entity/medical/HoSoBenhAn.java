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

/**
 * Hồ sơ bệnh án của 1 lượt khám (ERD: HoSoBenhAn): 1-1 với lịch hẹn (quy tắc #7), kèm đơn thuốc.
 * {@code maTokenSoKham} là token của link/QR Sổ khám bệnh (EXAM-03, quy tắc #8).
 * <p>
 * DOANTOTNGH-11 thêm lý do khám, sinh hiệu, khám lâm sàng, mã ICD-10 của chẩn đoán chính và chẩn đoán kèm theo: đều không
 * bắt buộc, hồ sơ cũ để trống.
 */
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

    // Sinh hiệu: mạch, nhịp thở lần/phút; nhiệt độ °C; huyết áp mmHg (có cả 2 số hoặc không số nào); cân nặng kg; chiều cao cm
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

    /** Mã ICD-10 của chẩn đoán chính (không bắt buộc); mã mạn tính cho kê thuốc tối đa 90 ngày thay vì 30. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_icd_chinh")
    private Icd10 icdChinh;

    /** Các bệnh kèm theo, chữ tự do. */
    @Column(name = "chan_doan_kem_theo", columnDefinition = "TEXT")
    private String chanDoanKemTheo;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    /** Gợi ý ngày tái khám. KHÔNG tự tạo lịch hẹn. */
    @Column(name = "ngay_tai_kham_de_xuat")
    private LocalDate ngayTaiKhamDeXuat;

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

    public void addChiTietDonThuoc(ChiTietDonThuoc chiTiet) {
        chiTietDonThuoc.add(chiTiet);
        chiTiet.setHoSoBenhAn(this);
    }

    public void removeChiTietDonThuoc(ChiTietDonThuoc chiTiet) {
        chiTietDonThuoc.remove(chiTiet);
        chiTiet.setHoSoBenhAn(null);
    }

}
