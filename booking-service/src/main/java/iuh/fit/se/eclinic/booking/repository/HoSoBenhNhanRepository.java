package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import jakarta.persistence.LockModeType;

public interface HoSoBenhNhanRepository extends JpaRepository<HoSoBenhNhan, Long> {

    Optional<HoSoBenhNhan> findByTaiKhoanId(Long taiKhoanId);

    /** Tra hồ sơ theo CCCD khi đặt lịch (BOOK-03): có rồi thì dùng lại, chưa có thì tạo mới. */
    Optional<HoSoBenhNhan> findByCccd(String cccd);

    boolean existsByCccd(String cccd);

    /**
     * Như {@link #findByCccd} nhưng khoá dòng hồ sơ (SELECT ... FOR UPDATE) trong transaction đặt lịch, để 2 request
     * đặt lịch cho cùng 1 bệnh nhân chạy lần lượt: kiểm tra trùng giờ và đếm số lịch còn hiệu lực mới chính xác.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoSoBenhNhan h where h.cccd = :cccd")
    Optional<HoSoBenhNhan> findByCccdForUpdate(String cccd);

    /**
     * Hồ sơ tạo khi bệnh nhân dưới 18 tuổi chưa có CCCD, tra theo khoá nhận diện (xem KhoaNhanDien); khoá dòng như
     * {@link #findByCccdForUpdate}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoSoBenhNhan h where h.khoaNhanDien = :khoaNhanDien")
    Optional<HoSoBenhNhan> findByKhoaNhanDienForUpdate(String khoaNhanDien);

    /** Khoá dòng hồ sơ khi quản trị viên duyệt / từ chối liên kết, để 2 thao tác cùng lúc chạy lần lượt. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoSoBenhNhan h where h.id = :id")
    Optional<HoSoBenhNhan> findByIdForUpdate(Long id);

    /** Hồ sơ theo trạng thái liên kết kèm tài khoản đang gắn (hàng chờ xác minh của quản trị viên), cũ nhất trước. */
    @Query(value = "select h from HoSoBenhNhan h join fetch h.taiKhoan where h.trangThaiLienKet = :trangThai"
            + " order by h.id asc",
            countQuery = "select count(h) from HoSoBenhNhan h where h.trangThaiLienKet = :trangThai")
    Page<HoSoBenhNhan> timTheoTrangThaiLienKet(TrangThaiLienKet trangThai, Pageable pageable);

    /**
     * Danh sách hồ sơ của quản trị viên, hồ sơ mới tạo trước, kèm tài khoản đang gắn (nếu có).
     *
     * @param mau       mẫu LIKE (escape '!') so với họ tên, số điện thoại, số bảo hiểm y tế; null = không lọc
     * @param cccd      số CCCD đầy đủ để so khớp hoàn toàn; null nếu từ khoá không phải 12 chữ số
     * @param trangThai null = mọi trạng thái liên kết
     */
    @Query(value = """
            select h from HoSoBenhNhan h left join fetch h.taiKhoan
            where (:trangThai is null or h.trangThaiLienKet = :trangThai)
              and (:mau is null or h.hoTen like :mau escape '!' or h.soDienThoai like :mau escape '!'
                   or h.soBaoHiemYTe like :mau escape '!' or h.cccd = :cccd)
            order by h.id desc
            """, countQuery = """
            select count(h) from HoSoBenhNhan h
            where (:trangThai is null or h.trangThaiLienKet = :trangThai)
              and (:mau is null or h.hoTen like :mau escape '!' or h.soDienThoai like :mau escape '!'
                   or h.soBaoHiemYTe like :mau escape '!' or h.cccd = :cccd)
            """)
    Page<HoSoBenhNhan> timChoQuanTri(String mau, String cccd, TrangThaiLienKet trangThai, Pageable pageable);

}
