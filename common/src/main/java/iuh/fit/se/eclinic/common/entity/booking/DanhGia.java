package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Đánh giá của bệnh nhân cho 1 lượt khám đã hoàn thành, là các cột danh_gia_* của chính lịch hẹn (mỗi lịch hẹn tối đa 1
 * đánh giá). Chỉ để đọc: gửi / sửa đánh giá bằng câu UPDATE riêng trong {@code DanhGiaRepository}, nên lịch hẹn không bị
 * tăng version hay đổi ngày cập nhật. Lịch hẹn chưa được đánh giá không phải là 1 {@code DanhGia}.
 */
@Getter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "lich_hen")
@SQLRestriction("danh_gia_so_sao is not null")
public class DanhGia {

    /** = id lịch hẹn. */
    @Id
    @Column(name = "id_lich_hen")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lich_hen")
    private LichHen lichHen;

    /** Bác sĩ của lượt khám. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", insertable = false, updatable = false)
    private BacSi bacSi;

    /** Tài khoản đã gửi đánh giá: bệnh nhân hoặc người đặt lịch. */
    @Column(name = "danh_gia_id_tai_khoan")
    private Long idTaiKhoan;

    /** 1..5. */
    @Column(name = "danh_gia_so_sao")
    private int soSao;

    @Column(name = "danh_gia_nhan_xet", length = 1000)
    private String nhanXet;

    @Column(name = "danh_gia_ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "danh_gia_ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;
}
