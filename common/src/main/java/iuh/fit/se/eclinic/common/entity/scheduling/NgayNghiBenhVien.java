package iuh.fit.se.eclinic.common.entity.scheduling;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
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
 * 1 ngày bệnh viện nghỉ (Tết, lễ, nghỉ đột xuất): không có ca làm việc nào còn hoạt động, không xếp ca vào được. Mỗi
 * ngày 1 dòng ({@code ngay} UNIQUE). Không sửa: muốn đổi lý do thì xoá rồi khai báo lại.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ngay_nghi_benh_vien")
@AttributeOverride(name = "id", column = @Column(name = "id_ngay_nghi"))
public class NgayNghiBenhVien extends CreatableEntity {

    @Column(name = "ngay", nullable = false, updatable = false)
    private LocalDate ngay;

    /** Lý do tiếng Việt (bắt buộc). */
    @Column(name = "ly_do", nullable = false, updatable = false, length = 200)
    private String lyDo;

    /** Lý do tiếng Anh; null = hiển thị {@link #lyDo}. */
    @Column(name = "ly_do_en", updatable = false, length = 200)
    private String lyDoEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_quan_tri_vien_tao", nullable = false, updatable = false)
    private QuanTriVien quanTriTao;

    public String lyDo(NgonNgu ngonNgu) {
        return ngonNgu.chon(lyDo, lyDoEn);
    }

}
