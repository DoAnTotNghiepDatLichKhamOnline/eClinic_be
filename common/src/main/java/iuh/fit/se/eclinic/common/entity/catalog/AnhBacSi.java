package iuh.fit.se.eclinic.common.entity.catalog;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ảnh giới thiệu của bác sĩ (ảnh làm việc, chứng chỉ) hiện trong trang chi tiết bác sĩ. Chỉ Admin thêm / sửa / xoá.
 * <p>
 * Ảnh đại diện không nằm ở đây mà ở {@code TaiKhoan.anhDaiDien}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "anh_bac_si")
@AttributeOverride(name = "id", column = @Column(name = "id_anh_bac_si"))
public class AnhBacSi extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", nullable = false)
    private BacSi bacSi;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loai", nullable = false, length = 20)
    private LoaiAnhBacSi loai;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    /** Mã ảnh trong kho ảnh, dùng để xoá. null = ảnh nằm ngoài kho (dữ liệu mẫu): xoá dòng là đủ. */
    @Column(name = "ma_luu_tru", length = 150)
    private String maLuuTru;

    @Column(name = "chu_thich", length = 200)
    private String chuThich;

    /** Thứ tự hiển thị, nhỏ đứng trước. */
    @Column(name = "thu_tu", nullable = false)
    private int thuTu;

}
