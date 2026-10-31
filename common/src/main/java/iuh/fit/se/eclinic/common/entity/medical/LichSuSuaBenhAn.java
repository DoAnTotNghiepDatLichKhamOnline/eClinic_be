package iuh.fit.se.eclinic.common.entity.medical;

import iuh.fit.se.eclinic.common.entity.LichSuThayDoi;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 1 lần sửa hồ sơ bệnh án: ai sửa, lúc nào ({@code ngayTao}), lý do, nội dung trước và sau khi sửa (JSON). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("BENH_AN")
public class LichSuSuaBenhAn extends LichSuThayDoi {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ho_so_benh_an", updatable = false)
    private HoSoBenhAn hoSoBenhAn;

    /** Phiên bản hồ sơ sau lần sửa này (2, 3, ...). */
    @Column(name = "phien_ban", updatable = false)
    private int phienBan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", updatable = false)
    private BacSi bacSi;

    @Column(name = "gia_tri_truoc", updatable = false, columnDefinition = "TEXT")
    private String noiDungTruoc;

    @Column(name = "gia_tri_sau", updatable = false, columnDefinition = "TEXT")
    private String noiDungSau;

}
