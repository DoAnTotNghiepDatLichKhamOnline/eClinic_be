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

    /** Ký tự thoát của {@code mau} trong {@link #timKiem}: đứng trước %, _ hoặc chính nó để so khớp đúng ký tự đó. */
    char KY_TU_THOAT = '!';

    /**
     * Danh sách tài khoản cho quản trị viên; tham số null là không lọc theo tiêu chí đó. Thứ tự do {@code pageable}.
     *
     * @param mau mẫu LIKE (đã có % ở 2 đầu, từ khoá đã được thoát bằng {@link #KY_TU_THOAT}) so với họ tên, email, số điện
     *            thoại; không phân biệt hoa thường và dấu nhờ collation của cột
     */
    @Query("select t from TaiKhoan t where (:vaiTro is null or t.vaiTro = :vaiTro)"
            + " and (:trangThai is null or t.trangThai = :trangThai)"
            + " and (:mau is null or t.hoTen like :mau escape '!' or t.email like :mau escape '!'"
            + " or t.soDienThoai like :mau escape '!')")
    Page<TaiKhoan> timKiem(String mau, VaiTro vaiTro, TrangThaiTaiKhoan trangThai, Pageable pageable);

}
