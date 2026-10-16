package iuh.fit.se.eclinic.common.entity.medical;

import iuh.fit.se.eclinic.common.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 1 dòng thuốc trong đơn (ERD: ChiTietDonThuoc). Thêm qua {@link HoSoBenhAn#addChiTietDonThuoc(ChiTietDonThuoc)}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chi_tiet_don_thuoc")
@AttributeOverride(name = "id", column = @Column(name = "id_chi_tiet_don"))
public class ChiTietDonThuoc extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ho_so_benh_an", nullable = false)
    private HoSoBenhAn hoSoBenhAn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_thuoc", nullable = false)
    private Thuoc thuoc;

    /** vd: 500mg/lần */
    @Column(name = "lieu_dung", length = 100)
    private String lieuDung;

    @Column(name = "so_lan_moi_ngay")
    private Integer soLanMoiNgay;

    /** Tối đa 30, hoặc 90 khi chẩn đoán chính mạn tính (kiểm tra trong medical-service). Null ở dòng cũ chưa sửa. */
    @Column(name = "so_ngay_dung")
    private Integer soNgayDung;

    /** Tổng số lượng cấp, theo đơn vị của thuốc. Null ở dòng ghi trước DOANTOTNGH-11 chưa sửa. */
    @Column(name = "so_luong")
    private Integer soLuong;

    /** vd: uống sau ăn */
    @Column(name = "ghi_chu_su_dung")
    private String ghiChuSuDung;

}
