package iuh.fit.se.eclinic.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.notification.ThongBao;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {

    /**
     * Thông báo của 1 tài khoản, mới nhất trước, kèm lịch hẹn liên quan (để lấy mã phiếu khám).
     *
     * @param chiChuaDoc true = chỉ lấy thông báo chưa đọc
     */
    @Query(value = """
            select t from ThongBao t left join fetch t.lichHen
            where t.taiKhoan.id = :idTaiKhoan and (:chiChuaDoc = false or t.daDoc = false)
            order by t.ngayTao desc, t.id desc
            """, countQuery = """
            select count(t) from ThongBao t
            where t.taiKhoan.id = :idTaiKhoan and (:chiChuaDoc = false or t.daDoc = false)
            """)
    Page<ThongBao> timCuaTaiKhoan(Long idTaiKhoan, boolean chiChuaDoc, Pageable pageable);

    /** Số thông báo chưa đọc (số trên biểu tượng chuông); dùng index (id_tai_khoan, da_doc, ngay_tao). */
    long countByTaiKhoanIdAndDaDocFalse(Long idTaiKhoan);

    @Query("select t from ThongBao t left join fetch t.lichHen where t.id = :id and t.taiKhoan.id = :idTaiKhoan")
    Optional<ThongBao> timCuaTaiKhoanTheoId(Long id, Long idTaiKhoan);

    @Modifying
    @Query("update ThongBao t set t.daDoc = true where t.taiKhoan.id = :idTaiKhoan and t.daDoc = false")
    int danhDauDaDocTatCa(Long idTaiKhoan);

    boolean existsByMaNguon(String maNguon);

}
