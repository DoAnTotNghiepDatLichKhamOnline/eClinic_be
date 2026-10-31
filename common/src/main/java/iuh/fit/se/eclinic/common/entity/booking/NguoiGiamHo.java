package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Người giám hộ khai khi đặt lịch cho bệnh nhân dưới 18 tuổi: các cột giam_ho_* của lịch hẹn. Ghi cùng dòng lịch hẹn lúc
 * đặt; sau đó chỉ {@code NguoiGiamHoRepository.ghiNgaySinh} được ghi (cột {@code updatable = false}).
 */
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class NguoiGiamHo {

    @Column(name = "giam_ho_ho_ten", length = 150, updatable = false)
    private String hoTen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "giam_ho_quan_he", length = 30, updatable = false)
    private QuanHeGiamHo quanHe;

    @Column(name = "giam_ho_so_dien_thoai", length = 20, updatable = false)
    private String soDienThoai;

    @Column(name = "giam_ho_cccd", length = 12, updatable = false)
    private String cccd;

    @Column(name = "giam_ho_ngay_sinh", updatable = false)
    private LocalDate ngaySinh;

    /** Bản sao cho lịch hẹn khác: 2 entity không dùng chung 1 đối tượng embeddable. */
    public NguoiGiamHo(NguoiGiamHo khac) {
        this.hoTen = khac.hoTen;
        this.quanHe = khac.quanHe;
        this.soDienThoai = khac.soDienThoai;
        this.cccd = khac.cccd;
        this.ngaySinh = khac.ngaySinh;
    }

}
