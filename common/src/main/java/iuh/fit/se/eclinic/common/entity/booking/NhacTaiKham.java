package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bệnh nhân đã được báo ngày tái khám nào của 1 hồ sơ bệnh án (không có trong ERD; V21). Mỗi hồ sơ bệnh án 1 dòng
 * (UNIQUE), ghi cùng transaction với thông báo / email chờ gửi. Job nhắc tái khám so dòng này với
 * {@code ho_so_benh_an.ngay_tai_kham_de_xuat} để biết bác sĩ vừa hẹn, đổi hay xoá ngày tái khám.
 * <p>
 * {@code idHoSoBenhAn} là số thường (DB có khoá ngoại): bảng của booking-service, hồ sơ bệnh án của medical-service.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "nhac_tai_kham")
@AttributeOverride(name = "id", column = @Column(name = "id_nhac"))
public class NhacTaiKham extends CreatableEntity {

    @Column(name = "id_ho_so_benh_an", nullable = false, unique = true)
    private Long idHoSoBenhAn;

    /** Ngày tái khám đã báo cho bệnh nhân; null = bác sĩ đã xoá ngày (hẹn lại thì báo như lần đầu). */
    @Column(name = "ngay_da_bao")
    private LocalDate ngayDaBao;

    /** Lần nhắc trước {@code ngayDaBao} đã được xử lý: đã nhắc, hoặc bỏ qua (xem {@code NhacTaiKhamService}). */
    @Column(name = "da_nhac_truoc", nullable = false)
    private boolean daNhacTruoc;

    public NhacTaiKham(Long idHoSoBenhAn) {
        this.idHoSoBenhAn = idHoSoBenhAn;
    }

}
