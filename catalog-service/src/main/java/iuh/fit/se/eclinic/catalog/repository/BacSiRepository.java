package iuh.fit.se.eclinic.catalog.repository;

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

    /**
     * Bác sĩ hiển thị công khai (DOC-03): đang công tác VÀ tài khoản đã kích hoạt. Vô hiệu hoá tài khoản ở
     * identity-service chỉ đổi trạng thái tài khoản, không đổi {@code bac_si.trang_thai}, nên phải kiểm cả hai.
     * So sánh tên theo collation của cột (không phân biệt hoa/thường và dấu). Sắp xếp theo tên.
     *
     * @param idChuyenKhoa null = mọi chuyên khoa
     * @param mauTen       mẫu LIKE đã escape bằng {@link #KY_TU_ESCAPE}, null = không lọc theo tên
     */
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

    /**
     * Danh bạ bác sĩ cho quản trị viên: mọi trạng thái, bác sĩ mới thêm đứng trước. Tham số null = không lọc.
     *
     * @param mau mẫu LIKE đã escape bằng {@link #KY_TU_ESCAPE}, so với họ tên, email, số điện thoại, số giấy phép
     */
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

    /**
     * Khoá dòng bác sĩ khi đổi trạng thái / chuyên khoa. booking-service khoá cùng dòng này khi xếp ca, nên "ngừng công
     * tác" và "xếp ca mới" chạy lần lượt. Không lấy sẵn tài khoản, chuyên khoa.
     */
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

}
