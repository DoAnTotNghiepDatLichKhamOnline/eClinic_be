package iuh.fit.se.eclinic.common.entity.booking;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.LichSuThayDoi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.LoaiNguoiThucHien;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 1 thao tác trên 1 lịch hẹn: việc gì, ai làm, lúc nào, lý do. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("LICH_HEN")
public class LichSuLichHen extends LichSuThayDoi {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lich_hen", updatable = false)
    private LichHen lichHen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "hanh_dong", updatable = false, length = 20)
    private SuKienLichHen suKien;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loai_nguoi_thuc_hien", updatable = false, length = 20)
    private LoaiNguoiThucHien loaiNguoiThucHien;

    /** Tài khoản đã làm; null khi {@code loaiNguoiThucHien} = KHACH. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan_thuc_hien", updatable = false)
    private TaiKhoan taiKhoanThucHien;

    /** DOI_LICH: lịch hẹn mới tạo ra. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lich_hen_lien_quan", updatable = false)
    private LichHen lichHenLienQuan;

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
