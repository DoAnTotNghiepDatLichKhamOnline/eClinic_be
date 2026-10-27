package iuh.fit.se.eclinic.booking.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.persistence.LockModeType;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

/** Chỉ đọc hồ sơ bác sĩ (bảng của catalog-service) để biết bác sĩ nào đang đăng nhập. */
public interface BacSiChiDocRepository extends Repository<BacSi, Long> {

    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

    /** Khoá dòng bác sĩ (SELECT ... */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BacSi b join fetch b.taiKhoan join fetch b.chuyenKhoa where b.id = :id")
    Optional<BacSi> khoaTheoId(Long id);

    /** Số bác sĩ đang công tác có tài khoản đã kích hoạt: đúng tập bác sĩ của danh sách công khai (catalog-service). */
    @Query("""
            select count(b) from BacSi b
            where b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and b.taiKhoan.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
            """)
    long demDangCongTacCoTaiKhoanHoatDong();

    /** Bác sĩ đang công tác (lọc theo chuyên khoa / bác sĩ, null = không lọc), lấy sẵn tài khoản và chuyên khoa. */
    @Query("""
            select b from BacSi b
              join fetch b.taiKhoan t
              join fetch b.chuyenKhoa c
            where b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and (:idChuyenKhoa is null or c.id = :idChuyenKhoa)
              and (:idBacSi is null or b.id = :idBacSi)
            order by t.hoTen, b.id
            """)
    List<BacSi> timDangCongTac(Long idChuyenKhoa, Long idBacSi);

    /** Các bác sĩ theo id (kể cả đã ngừng công tác), lấy sẵn tài khoản và chuyên khoa. */
    @Query("select b from BacSi b join fetch b.taiKhoan join fetch b.chuyenKhoa where b.id in :cacId")
    List<BacSi> timTheoCacId(Collection<Long> cacId);

}
