package iuh.fit.fe.be_websatlichkham.common.entity.medical;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.CreatableEntity;
import iuh.fit.fe.be_websatlichkham.common.entity.booking.HoSoBenhNhan;
import iuh.fit.fe.be_websatlichkham.common.entity.booking.LichHen;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;
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

    @Column(name = "chan_doan", columnDefinition = "TEXT")
    private String chanDoan;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    /** Gợi ý ngày tái khám. KHÔNG tự tạo lịch hẹn. */
    @Column(name = "ngay_tai_kham_de_xuat")
    private LocalDate ngayTaiKhamDeXuat;

    @Column(name = "ma_token_so_kham", nullable = false, unique = true, length = 64)
    private String maTokenSoKham;

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
