package iuh.fit.se.eclinic.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.notification.EmailGui;
import iuh.fit.se.eclinic.common.enums.TrangThaiEmail;

public interface EmailGuiRepository extends JpaRepository<EmailGui, Long> {

    boolean existsByMaNguon(String maNguon);

    /** Email đến hạn gửi, cũ nhất trước; dùng index (trang_thai, lan_thu_ke_tiep). */
    List<EmailGui> findByTrangThaiAndLanThuKeTiepLessThanEqualOrderByIdAsc(TrangThaiEmail trangThai,
            LocalDateTime bayGio, Limit gioiHan);

    /** Chỉ đổi dòng còn CHO_GUI: 1 email đã DA_GUI không bao giờ bị ghi đè. */
    @Transactional
    @Modifying
    @Query("""
            update EmailGui e set e.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiEmail.DA_GUI,
                e.ngayGui = :bayGio, e.loiCuoi = null
            where e.id = :id and e.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiEmail.CHO_GUI
            """)
    int danhDauDaGui(Long id, LocalDateTime bayGio);

    /** Ghi 1 lần gửi lỗi. */
    @Transactional
    @Modifying
    @Query("""
            update EmailGui e set e.soLanThu = :soLanThu, e.trangThai = :trangThai, e.lanThuKeTiep = :lanThuKeTiep,
                e.loiCuoi = :loi
            where e.id = :id and e.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiEmail.CHO_GUI
            """)
    int ghiLoi(Long id, int soLanThu, TrangThaiEmail trangThai, LocalDateTime lanThuKeTiep, String loi);

    @Transactional
    @Modifying
    @Query("""
            delete from EmailGui e
            where e.trangThai <> iuh.fit.se.eclinic.common.enums.TrangThaiEmail.CHO_GUI and e.ngayTao < :truoc
            """)
    int xoaDaXongTruoc(LocalDateTime truoc);

}
