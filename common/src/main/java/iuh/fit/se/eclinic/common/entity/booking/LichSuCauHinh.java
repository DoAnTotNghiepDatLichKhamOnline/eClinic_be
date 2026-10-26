package iuh.fit.se.eclinic.common.entity.booking;

import org.hibernate.annotations.Immutable;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
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
 * 1 trường của {@link CauHinhHeThong} đổi giá trị trong 1 lần lưu: tên trường (như trong API), giá trị cũ, mới, ai lưu
 * ({@code quanTri}), lúc nào ({@code ngayTao}). CHỈ THÊM: {@link Immutable} nên Hibernate bỏ qua mọi thay đổi trên dòng
 * đã lưu.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "lich_su_cau_hinh")
@AttributeOverride(name = "id", column = @Column(name = "id_lich_su_cau_hinh"))
public class LichSuCauHinh extends CreatableEntity {

    @Column(name = "ten_truong", nullable = false, updatable = false, length = 60)
    private String tenTruong;

    @Column(name = "gia_tri_cu", nullable = false, updatable = false, length = 30)
    private String giaTriCu;

    @Column(name = "gia_tri_moi", nullable = false, updatable = false, length = 30)
    private String giaTriMoi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_quan_tri_vien", nullable = false, updatable = false)
    private QuanTriVien quanTri;

}
