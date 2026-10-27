package iuh.fit.se.eclinic.catalog.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import jakarta.persistence.LockModeType;

public interface BacSiRepository extends JpaRepository<BacSi, Long> {

    /** Ký tự escape của LIKE trong {@link #timCongKhai}: mẫu tên phải escape %, _ và chính ký tự này. */
    char KY_TU_ESCAPE = '!';

    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

    List<BacSi> findByChuyenKhoaId(Long chuyenKhoaId);

    long countByChuyenKhoaId(Long chuyenKhoaId);

    /** Bác sĩ hiển thị công khai: đang công tác và tài khoản đã kích hoạt. */
    @Query(value = """
            select b from BacSi b
              join fetch b.taiKhoan t
              join fetch b.chuyenKhoa c
            where b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and (:idChuyenKhoa is null or c.id = :idChuyenKhoa)
              and (:mauTen is null or t.hoTen like :mauTen escape '!')
            order by t.hoTen, b.id
            """, countQuery = """
            select count(b) from BacSi b
              join b.taiKhoan t
            where b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and (:idChuyenKhoa is null or b.chuyenKhoa.id = :idChuyenKhoa)
              and (:mauTen is null or t.hoTen like :mauTen escape '!')
            """)
    Page<BacSi> timCongKhai(Long idChuyenKhoa, String mauTen, Pageable pageable);

    /** Điều kiện "bác sĩ b còn ít nhất 1 lượt khám đặt được" trong khoảng ngày và giờ của lượt. */
    String CON_LUOT_DAT_DUOC = """
              and exists (select 1 from KhungGioKham k join k.lichLamViec l
                          where l.bacSi = b
                            and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
                            and k.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio.CON_TRONG
                            and k.gioBatDau >= :moc
                            and l.ngayLamViec between :tuNgay and :denNgay
                            and extract(hour from k.gioBatDau) >= :gioTu
                            and extract(hour from k.gioBatDau) < :gioDen)
            """;

    /** Như {@link #timCongKhai}, chỉ giữ bác sĩ còn lượt khám đặt được. */
    @Query(value = """
            select b from BacSi b
              join fetch b.taiKhoan t
              join fetch b.chuyenKhoa c
            where b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and (:idChuyenKhoa is null or c.id = :idChuyenKhoa)
              and (:mauTen is null or t.hoTen like :mauTen escape '!')
            """ + CON_LUOT_DAT_DUOC + """
            order by t.hoTen, b.id
            """, countQuery = """
            select count(b) from BacSi b
              join b.taiKhoan t
            where b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and (:idChuyenKhoa is null or b.chuyenKhoa.id = :idChuyenKhoa)
              and (:mauTen is null or t.hoTen like :mauTen escape '!')
            """ + CON_LUOT_DAT_DUOC)
    Page<BacSi> timCongKhaiConCho(Long idChuyenKhoa, String mauTen, LocalDateTime moc, LocalDate tuNgay,
            LocalDate denNgay, int gioTu, int gioDen, Pageable pageable);

    /** Như {@link #timCongKhai}: bác sĩ ngừng công tác hoặc tài khoản không hoạt động thì coi như không có. */
    @Query("""
            select b from BacSi b
              join fetch b.taiKhoan t
              join fetch b.chuyenKhoa
            where b.id = :id
              and b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
            """)
    Optional<BacSi> timCongKhaiTheoId(Long id);

    /** Danh bạ bác sĩ cho quản trị viên: mọi trạng thái, bác sĩ mới thêm đứng trước. */
    @Query(value = """
            select b from BacSi b
              join fetch b.taiKhoan t
              join fetch b.chuyenKhoa c
            where (:idChuyenKhoa is null or c.id = :idChuyenKhoa)
              and (:trangThai is null or b.trangThai = :trangThai)
              and (:mau is null or t.hoTen like :mau escape '!' or t.email like :mau escape '!'
                   or t.soDienThoai like :mau escape '!' or b.soGiayPhep like :mau escape '!')
            order by b.id desc
            """, countQuery = """
            select count(b) from BacSi b
              join b.taiKhoan t
            where (:idChuyenKhoa is null or b.chuyenKhoa.id = :idChuyenKhoa)
              and (:trangThai is null or b.trangThai = :trangThai)
              and (:mau is null or t.hoTen like :mau escape '!' or t.email like :mau escape '!'
                   or t.soDienThoai like :mau escape '!' or b.soGiayPhep like :mau escape '!')
            """)
    Page<BacSi> timChoQuanTri(String mau, Long idChuyenKhoa, TrangThaiBacSi trangThai, Pageable pageable);

    /** Khoá dòng bác sĩ khi đổi trạng thái, chuyên khoa; booking-service khoá cùng dòng khi xếp ca. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BacSi b where b.id = :id")
    Optional<BacSi> findByIdForUpdate(Long id);

    boolean existsBySoGiayPhep(String soGiayPhep);

    boolean existsBySoGiayPhepAndIdNot(String soGiayPhep, Long id);

    /** Cho quản trị viên: bác sĩ theo id, không lọc trạng thái, lấy sẵn tài khoản và chuyên khoa. */
    @Query("""
            select b from BacSi b
              join fetch b.taiKhoan
              join fetch b.chuyenKhoa
            where b.id = :id
            """)
    Optional<BacSi> timKemTaiKhoanVaChuyenKhoa(Long id);

    /** Bác sĩ của 1 tài khoản (bác sĩ đang đăng nhập), lấy sẵn tài khoản để kiểm tra trạng thái. */
    @Query("""
            select b from BacSi b
              join fetch b.taiKhoan t
            where t.id = :idTaiKhoan
            """)
    Optional<BacSi> timTheoTaiKhoanKemTaiKhoan(Long idTaiKhoan);

}
