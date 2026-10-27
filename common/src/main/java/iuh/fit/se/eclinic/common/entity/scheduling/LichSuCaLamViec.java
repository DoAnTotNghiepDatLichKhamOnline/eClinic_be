package iuh.fit.se.eclinic.common.entity.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.enums.HanhDongCa;
import iuh.fit.se.eclinic.common.enums.NguonThayDoiCa;
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

/** 1 lần xếp / sửa / hủy 1 ca làm việc: ai làm, lúc nào, từ đâu, lý do, và ảnh ca sau thay đổi. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "lich_su_ca_lam_viec")
@AttributeOverride(name = "id", column = @Column(name = "id_lich_su_ca"))
public class LichSuCaLamViec extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lich_lam_viec", nullable = false, updatable = false)
    private LichLamViec lichLamViec;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "hanh_dong", nullable = false, updatable = false, length = 20)
    private HanhDongCa hanhDong;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "nguon", nullable = false, updatable = false, length = 20)
    private NguonThayDoiCa nguon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_quan_tri_vien", nullable = false, updatable = false)
    private QuanTriVien quanTri;

    /** Yêu cầu đổi ca / xin nghỉ được duyệt; chỉ khi {@code nguon} = DUYET_YEU_CAU. */
    @Column(name = "id_yeu_cau", updatable = false)
    private Long idYeuCau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bac_si", nullable = false, updatable = false)
    private BacSi bacSi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_phong_kham", nullable = false, updatable = false)
    private PhongKham phongKham;

    @Column(name = "ngay_lam_viec", nullable = false, updatable = false)
    private LocalDate ngayLamViec;

    @Column(name = "gio_bat_dau", nullable = false, updatable = false)
    private LocalTime gioBatDau;

    @Column(name = "gio_ket_thuc", nullable = false, updatable = false)
    private LocalTime gioKetThuc;

    @Column(name = "so_luot_toi_da_moi_gio", nullable = false, updatable = false)
    private Integer soLuotToiDaMoiGio;

    @Column(name = "thoi_luong_luot_phut", nullable = false, updatable = false)
    private Integer thoiLuongLuotPhut;

    /** Lý do hủy: câu quản trị viên gõ, hoặc câu của hệ thống dạng {@code {heThong.<khoa>}} (VanBanHeThong). */
    @Column(name = "ly_do", updatable = false, columnDefinition = "TEXT")
    private String lyDo;

    /** Dòng lịch sử mang ảnh hiện tại của ca. */
    public static LichSuCaLamViec anhCua(LichLamViec ca, HanhDongCa hanhDong, NguonThayDoiCa nguon,
            QuanTriVien quanTri) {
        LichSuCaLamViec dong = new LichSuCaLamViec();
        dong.setLichLamViec(ca);
        dong.setHanhDong(hanhDong);
        dong.setNguon(nguon);
        dong.setQuanTri(quanTri);
        dong.setBacSi(ca.getBacSi());
        dong.setPhongKham(ca.getPhongKham());
        dong.setNgayLamViec(ca.getNgayLamViec());
        dong.setGioBatDau(ca.getGioBatDau());
        dong.setGioKetThuc(ca.getGioKetThuc());
        dong.setSoLuotToiDaMoiGio(ca.getSoLuotToiDaMoiGio());
        dong.setThoiLuongLuotPhut(ca.getThoiLuongLuotPhut());
        return dong;
    }

}
