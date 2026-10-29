package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import jakarta.persistence.LockModeType;

/**
 * Ngày tái khám đã báo là 2 cột tai_kham_* của hồ sơ bệnh án (bảng của medical-service); booking-service chỉ ghi 2 cột
 * này, bằng các câu UPDATE riêng dưới đây.
 */
public interface NhacTaiKhamRepository extends Repository<HoSoBenhAn, Long> {

    /** Khoá dòng hồ sơ bệnh án trước khi quyết định báo / nhắc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from HoSoBenhAn b where b.id = :id")
    Optional<HoSoBenhAn> timDeKhoa(Long id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update ho_so_benh_an set tai_kham_ngay_da_bao = :ngay, tai_kham_da_nhac_truoc = :daNhacTruoc
            where id_ho_so_benh_an = :id
            """, nativeQuery = true)
    int ghiDaBao(Long id, LocalDate ngay, boolean daNhacTruoc);

    @Modifying(flushAutomatically = true)
    @Query(value = "update ho_so_benh_an set tai_kham_da_nhac_truoc = true where id_ho_so_benh_an = :id",
            nativeQuery = true)
    int ghiDaNhacTruoc(Long id);

    /** Bác sĩ đã xoá ngày tái khám: quên ngày đã báo, để sau này hẹn lại (kể cả đúng ngày cũ) thì báo như lần đầu. */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            update ho_so_benh_an set tai_kham_ngay_da_bao = null, tai_kham_da_nhac_truoc = false
            where tai_kham_ngay_da_bao is not null and ngay_tai_kham_de_xuat is null
            """, nativeQuery = true)
    int quenNgayDaXoa();

}
