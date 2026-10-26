package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.booking.NhacTaiKham;
import jakarta.persistence.LockModeType;

public interface NhacTaiKhamRepository extends JpaRepository<NhacTaiKham, Long> {

    /** Khoá dòng của 1 hồ sơ bệnh án trước khi quyết định báo / nhắc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NhacTaiKham n where n.idHoSoBenhAn = :idHoSoBenhAn")
    Optional<NhacTaiKham> timDeKhoa(Long idHoSoBenhAn);

    /**
     * Bác sĩ đã xoá ngày tái khám: quên ngày đã báo, để sau này hẹn lại (kể cả đúng ngày cũ) thì báo như lần đầu.
     *
     * @return số dòng vừa đổi
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update NhacTaiKham n set n.ngayDaBao = null, n.daNhacTruoc = false
            where n.ngayDaBao is not null
              and exists (select 1 from HoSoBenhAn b where b.id = n.idHoSoBenhAn and b.ngayTaiKhamDeXuat is null)
            """)
    int quenNgayDaXoa();

}
