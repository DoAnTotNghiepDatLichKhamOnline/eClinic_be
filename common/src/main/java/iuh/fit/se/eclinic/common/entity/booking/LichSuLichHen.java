package iuh.fit.se.eclinic.common.entity.booking;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.CreatableEntity;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.LoaiNguoiThucHien;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
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

/** 1 thao tác trên 1 lịch hẹn: việc gì, ai làm, lúc nào, lý do. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "lich_su_lich_hen")
@AttributeOverride(name = "id", column = @Column(name = "id_lich_su_lich_hen"))
public class LichSuLichHen extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lich_hen", nullable = false, updatable = false)
    private LichHen lichHen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "su_kien", nullable = false, updatable = false, length = 20)
    private SuKienLichHen suKien;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loai_nguoi_thuc_hien", nullable = false, updatable = false, length = 20)
    private LoaiNguoiThucHien loaiNguoiThucHien;

    /** Tài khoản đã làm; null khi {@code loaiNguoiThucHien} = KHACH. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan_thuc_hien", updatable = false)
    private TaiKhoan taiKhoanThucHien;

    /** DOI_LICH: lịch hẹn mới tạo ra. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lich_hen_lien_quan", updatable = false)
    private LichHen lichHenLienQuan;

    /** Câu người dùng gõ, hoặc câu của hệ thống dạng {@code {heThong.<khoa>}} (VanBanHeThong). */
    @Column(name = "ly_do", updatable = false, columnDefinition = "TEXT")
    private String lyDo;

    public static LichSuLichHen tao(LichHen lichHen, SuKienLichHen suKien, LoaiNguoiThucHien loai,
            TaiKhoan taiKhoan, String lyDo, LichHen lienQuan) {
        LichSuLichHen dong = new LichSuLichHen();
        dong.setLichHen(lichHen);
        dong.setSuKien(suKien);
        dong.setLoaiNguoiThucHien(loai);
        dong.setTaiKhoanThucHien(taiKhoan);
        dong.setLyDo(lyDo);
        dong.setLichHenLienQuan(lienQuan);
        return dong;
    }

}
