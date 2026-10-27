package iuh.fit.se.eclinic.common.entity.booking;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Quy tắc đặt lịch / xếp ca do quản trị viên sửa. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cau_hinh_he_thong")
public class CauHinhHeThong {

    public static final int ID = 1;

    @Id
    @Column(name = "id_cau_hinh")
    private Integer id;

    @Column(name = "dat_truoc_toi_thieu_phut", nullable = false)
    private int datTruocToiThieuPhut;

    @Column(name = "so_ngay_dat_truoc_toi_da", nullable = false)
    private int soNgayDatTruocToiDa;

    @Column(name = "so_lich_hieu_luc_toi_da_moi_ho_so", nullable = false)
    private int soLichHieuLucToiDaMoiHoSo;

    @Column(name = "so_lich_hieu_luc_toi_da_moi_sdt", nullable = false)
    private int soLichHieuLucToiDaMoiSoDienThoai;

    @Column(name = "huy_doi_truoc_toi_thieu_phut", nullable = false)
    private int huyDoiTruocToiThieuPhut;

    @Column(name = "so_lan_doi_lich_toi_da", nullable = false)
    private int soLanDoiLichToiDa;

    @Column(name = "gui_yeu_cau_truoc_toi_thieu_gio", nullable = false)
    private int guiYeuCauTruocToiThieuGio;

    @Column(name = "so_ngay_xep_truoc_toi_da", nullable = false)
    private int soNgayXepTruocToiDa;

    @Column(name = "so_gio_toi_da_moi_tuan", nullable = false)
    private int soGioToiDaMoiTuan;

    @Column(name = "so_ngay_duoc_sua_danh_gia", nullable = false)
    private int soNgayDuocSuaDanhGia;

    /** Tăng 1 mỗi lần lưu có thay đổi; lần lưu gửi phiên bản cũ bị từ chối (409 DU_LIEU_DA_THAY_DOI). */
    @Column(name = "phien_ban", nullable = false)
    private int phienBan;

    /** Lần lưu gần nhất; null = chưa ai sửa, giá trị lấy từ biến môi trường. */
    @Column(name = "ngay_sua")
    private LocalDateTime ngaySua;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_quan_tri_vien_sua")
    private QuanTriVien quanTriSua;

}
