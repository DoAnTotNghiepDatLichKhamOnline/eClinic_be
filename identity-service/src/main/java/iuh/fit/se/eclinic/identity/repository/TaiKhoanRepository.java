package iuh.fit.se.eclinic.identity.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {

    Optional<TaiKhoan> findByEmail(String email);

    Optional<TaiKhoan> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    boolean existsBySoDienThoai(String soDienThoai);

    boolean existsBySoDienThoaiAndIdNot(String soDienThoai, Long id);

    boolean existsByVaiTro(VaiTro vaiTro);

    /** Ký tự thoát của {@code mau} trong {@link #timKiem}. */
    char KY_TU_THOAT = '!';

    /** Danh sách tài khoản cho quản trị viên; tham số null là không lọc theo tiêu chí đó. */
    @Query("select t from TaiKhoan t where (:vaiTro is null or t.vaiTro = :vaiTro)"
            + " and (:trangThai is null or t.trangThai = :trangThai)"
            + " and (:mau is null or t.hoTen like :mau escape '!' or t.email like :mau escape '!'"
            + " or t.soDienThoai like :mau escape '!')")
    Page<TaiKhoan> timKiem(String mau, VaiTro vaiTro, TrangThaiTaiKhoan trangThai, Pageable pageable);

}
