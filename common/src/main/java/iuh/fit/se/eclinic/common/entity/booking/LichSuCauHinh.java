package iuh.fit.se.eclinic.common.entity.booking;

import iuh.fit.se.eclinic.common.entity.LichSuThayDoi;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 1 trường của {@link CauHinhHeThong} đổi giá trị trong 1 lần lưu: giá trị cũ, mới, ai lưu, lúc nào. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("CAU_HINH")
public class LichSuCauHinh extends LichSuThayDoi {

    @Column(name = "ten_truong", updatable = false, length = 60)
    private String tenTruong;

    @Column(name = "gia_tri_truoc", updatable = false, columnDefinition = "TEXT")
    private String giaTriCu;

    @Column(name = "gia_tri_sau", updatable = false, columnDefinition = "TEXT")
    private String giaTriMoi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_quan_tri_vien", updatable = false)
    private QuanTriVien quanTri;

}
