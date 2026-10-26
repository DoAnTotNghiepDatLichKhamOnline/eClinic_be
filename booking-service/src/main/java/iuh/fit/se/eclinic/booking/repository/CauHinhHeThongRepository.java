package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;
import jakarta.persistence.LockModeType;

/**
 * Dòng cấu hình duy nhất (id = {@link CauHinhHeThong#ID}). Không có save / delete: dòng chỉ được thêm bằng
 * {@link #themNeuChuaCo}, sau đó chỉ sửa qua entity đã khoá.
 */
public interface CauHinhHeThongRepository extends Repository<CauHinhHeThong, Integer> {

    Optional<CauHinhHeThong> findById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CauHinhHeThong c where c.id = :id")
    Optional<CauHinhHeThong> findByIdForUpdate(Integer id);

    /**
     * Khoá chia sẻ (MySQL {@code FOR SHARE}) dòng cấu hình: dùng làm khoá xếp ca toàn viện. Mọi thao tác tạo ca giữ khoá
     * này, khai báo ngày nghỉ giữ khoá ghi ({@link #findByIdForUpdate}), nên 2 việc không chạy xen nhau (xem
     * {@code QuanLyCaService#giuKhoaXepCa}).
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from CauHinhHeThong c where c.id = :id")
    Optional<CauHinhHeThong> khoaChiaSe(Integer id);

    /** Kèm quản trị viên lưu gần nhất (có thể chưa có) và tài khoản của người đó. */
    @Query("""
            select c from CauHinhHeThong c
              left join fetch c.quanTriSua q
              left join fetch q.taiKhoan
            where c.id = :id
            """)
    Optional<CauHinhHeThong> timKemQuanTriSua(Integer id);

    /**
     * Thêm dòng id 1 nếu chưa có; đã có thì không đổi gì (không ghi đè giá trị quản trị viên đã sửa). 2 lần gọi cùng lúc
     * vẫn an toàn vì khoá chính quyết định, không phải 1 lần đọc trước. Dùng ON DUPLICATE KEY thay cho INSERT IGNORE để
     * lỗi khác (NULL, sai kiểu) vẫn báo ra.
     */
    @Modifying
    @Query(value = """
            insert into cau_hinh_he_thong (id_cau_hinh, dat_truoc_toi_thieu_phut, so_ngay_dat_truoc_toi_da,
                so_lich_hieu_luc_toi_da_moi_ho_so, so_lich_hieu_luc_toi_da_moi_sdt, huy_doi_truoc_toi_thieu_phut,
                so_lan_doi_lich_toi_da, gui_yeu_cau_truoc_toi_thieu_gio, so_ngay_xep_truoc_toi_da, so_gio_toi_da_moi_tuan,
                so_ngay_duoc_sua_danh_gia, phien_ban)
            values (1, :datTruocToiThieuPhut, :soNgayDatTruocToiDa, :soLichHieuLucToiDaMoiHoSo,
                :soLichHieuLucToiDaMoiSoDienThoai, :huyDoiTruocToiThieuPhut, :soLanDoiLichToiDa, :guiYeuCauTruocToiThieuGio,
                :soNgayXepTruocToiDa, :soGioToiDaMoiTuan, :soNgayDuocSuaDanhGia, 1)
            on duplicate key update id_cau_hinh = id_cau_hinh
            """, nativeQuery = true)
    int themNeuChuaCo(int datTruocToiThieuPhut, int soNgayDatTruocToiDa, int soLichHieuLucToiDaMoiHoSo,
            int soLichHieuLucToiDaMoiSoDienThoai, int huyDoiTruocToiThieuPhut, int soLanDoiLichToiDa,
            int guiYeuCauTruocToiThieuGio, int soNgayXepTruocToiDa, int soGioToiDaMoiTuan, int soNgayDuocSuaDanhGia);

}
