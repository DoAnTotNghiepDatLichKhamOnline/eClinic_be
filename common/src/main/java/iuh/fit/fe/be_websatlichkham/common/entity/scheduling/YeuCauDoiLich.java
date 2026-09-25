package iuh.fit.fe.be_websatlichkham.common.entity.scheduling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.PhongKham;
import iuh.fit.fe.be_websatlichkham.common.entity.identity.QuanTriVien;
import iuh.fit.fe.be_websatlichkham.common.enums.LoaiYeuCauDoiLich;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiYeuCau;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * Yêu cầu đổi lịch làm việc do bác sĩ gửi, Admin duyệt/từ chối (ERD: YeuCauDoiLich — SCHED-02, SCHED-03).
 * <p>
 * Mỗi ca chỉ có tối đa 1 yêu cầu CHO_DUYET (quy tắc #15): DB chặn bằng cột generated
 * {@code id_lich_lam_viec_cho_duyet} (không map trong entity) + UNIQUE.
 * <p>
 * Khác ERD: thêm {@code loaiYeuCau}, {@code phongKhamMongMuon}, {@code ghiChuXuLy} (.md mục 15.4);
 * {@code thoiGianMongMuon} được tách thành ngày + giờ bắt đầu + giờ kết thúc mong muốn (ca mới cần cả giờ kết thúc).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "yeu_cau_doi_lich")
@AttributeOverride(name = "id", column = @Column(name = "id_yeu_cau"))
public class YeuCauDoiLich extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", nullable = false)
    private BacSi bacSi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lich_lam_viec", nullable = false)
    private LichLamViec lichLamViec;

    /** Admin đã duyệt/từ chối; NULL khi còn CHO_DUYET. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_xu_ly")
    private QuanTriVien adminXuLy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loai_yeu_cau", nullable = false, length = 20)
    private LoaiYeuCauDoiLich loaiYeuCau;

    @Column(name = "ly_do", nullable = false, columnDefinition = "TEXT")
    private String lyDo;

    /** Bắt buộc với DOI_CA (DB CHECK), NULL với XIN_NGHI. */
    @Column(name = "ngay_mong_muon")
    private LocalDate ngayMongMuon;

    @Column(name = "gio_bat_dau_mong_muon")
    private LocalTime gioBatDauMongMuon;

    @Column(name = "gio_ket_thuc_mong_muon")
    private LocalTime gioKetThucMongMuon;

    /** NULL = giữ phòng khám hiện tại. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phong_kham_mong_muon")
    private PhongKham phongKhamMongMuon;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiYeuCau trangThai = TrangThaiYeuCau.CHO_DUYET;

    /** Ghi chú của Admin khi xử lý — bắt buộc khi từ chối (SCHED-03). */
    @Column(name = "ghi_chu_xu_ly", columnDefinition = "TEXT")
    private String ghiChuXuLy;

    @CreatedDate
    @Column(name = "ngay_gui", nullable = false, updatable = false)
    private LocalDateTime ngayGui;

    @Column(name = "ngay_xu_ly")
    private LocalDateTime ngayXuLy;

}
