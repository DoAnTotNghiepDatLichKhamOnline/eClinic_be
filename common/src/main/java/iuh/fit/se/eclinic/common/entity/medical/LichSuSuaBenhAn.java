package iuh.fit.se.eclinic.common.entity.medical;

import org.hibernate.annotations.Immutable;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
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

/** 1 lần sửa hồ sơ bệnh án: ai sửa, lúc nào ({@code ngayTao}), lý do, nội dung trước và sau khi sửa (JSON). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "lich_su_sua_benh_an")
@AttributeOverride(name = "id", column = @Column(name = "id_lich_su_sua"))
public class LichSuSuaBenhAn extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ho_so_benh_an", nullable = false, updatable = false)
    private HoSoBenhAn hoSoBenhAn;

    /** Phiên bản hồ sơ sau lần sửa này (2, 3, ...). */
    @Column(name = "phien_ban", nullable = false, updatable = false)
    private int phienBan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", nullable = false, updatable = false)
    private BacSi bacSi;

    @Column(name = "ly_do", nullable = false, updatable = false, length = 500)
    private String lyDo;

    @Column(name = "noi_dung_truoc", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String noiDungTruoc;

    @Column(name = "noi_dung_sau", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String noiDungSau;

}
