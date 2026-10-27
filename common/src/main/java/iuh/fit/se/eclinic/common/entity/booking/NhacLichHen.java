package iuh.fit.se.eclinic.common.entity.booking;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 1 mốc nhắc lịch khám đã được xử lý cho 1 lịch hẹn. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "nhac_lich_hen")
@AttributeOverride(name = "id", column = @Column(name = "id_nhac"))
public class NhacLichHen extends CreatableEntity {

    @Column(name = "id_lich_hen", nullable = false)
    private Long idLichHen;

    /** Mốc nhắc: số phút trước giờ khám (vd 4320 = 3 ngày). */
    @Column(name = "so_phut_truoc", nullable = false)
    private int soPhutTruoc;

    /** false = mốc đã ghi nhận nhưng không nhắc (đặt lịch muộn, hoặc nhường mốc gần hơn). */
    @Column(name = "da_gui", nullable = false)
    private boolean daGui;

    public NhacLichHen(Long idLichHen, int soPhutTruoc, boolean daGui) {
        this.idLichHen = idLichHen;
        this.soPhutTruoc = soPhutTruoc;
        this.daGui = daGui;
    }

}
