package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

public interface LichHenRepository extends JpaRepository<LichHen, Long> {

    /** Lịch hẹn kèm mọi thứ phiếu khám / danh sách lịch hẹn hiển thị; các quan hệ đều là *-to-one nên phân trang được. */
    String LICH_HEN_KEM_CHI_TIET = """
            select l from LichHen l
            join fetch l.hoSoBenhNhan
            left join fetch l.nguoiGiamHo
            join fetch l.bacSi b
            join fetch b.taiKhoan
            join fetch b.chuyenKhoa
            join fetch l.phongKham
            join fetch l.khungGio k
            join fetch k.lichLamViec
            """;

    List<LichHen> findByHoSoBenhNhanIdOrderByNgayTaoDesc(Long hoSoBenhNhanId);

    List<LichHen> findByBacSiIdAndTrangThaiOrderByNgayTaoDesc(Long bacSiId, TrangThaiLichHen trangThai);

    boolean existsByKhungGioIdAndTrangThaiIn(Long khungGioId, List<TrangThaiLichHen> trangThais);

    /**
     * Lịch hẹn theo mã phiếu khám, kèm mọi thứ phiếu khám hiển thị (hồ sơ, người giám hộ, bác sĩ + tài khoản + chuyên
     * khoa, phòng khám, lượt khám + ca). Cột mã dùng collation không phân biệt hoa thường nên nơi gọi phải so lại
     * chính xác bằng {@code equals}.
     */
    @Query(LICH_HEN_KEM_CHI_TIET + "where l.maTokenPhieuKham = :maPhieuKham")
    Optional<LichHen> timTheoMaPhieuKham(String maPhieuKham);

    /**
     * BOOK-07: lịch hẹn do tài khoản này đặt khi đã đăng nhập (cho bản thân hoặc người thân), giờ khám muộn nhất trước.
     * Thứ tự viết trong JPQL nên {@code pageable} không kèm Sort.
     */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where l.taiKhoanDat.id = :idTaiKhoan order by k.gioBatDau desc, l.id desc",
            countQuery = "select count(l) from LichHen l where l.taiKhoanDat.id = :idTaiKhoan")
    Page<LichHen> timCuaTaiKhoan(Long idTaiKhoan, Pageable pageable);

    /** Lịch sắp tới của tài khoản: còn hiệu lực ({@code trangThai}) và lượt khám chưa kết thúc; gần nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + """
            where l.taiKhoanDat.id = :idTaiKhoan
              and l.trangThai in :trangThai
              and k.gioKetThuc > :bayGio
            order by k.gioBatDau asc, l.id asc
            """, countQuery = """
            select count(l) from LichHen l
            where l.taiKhoanDat.id = :idTaiKhoan
              and l.trangThai in :trangThai
              and l.khungGio.gioKetThuc > :bayGio
            """)
    Page<LichHen> timSapToiCuaTaiKhoan(Long idTaiKhoan, Collection<TrangThaiLichHen> trangThai, LocalDateTime bayGio,
            Pageable pageable);

    /** Phần còn lại của {@link #timSapToiCuaTaiKhoan}: lịch đã khám, đã hủy, bị từ chối hoặc đã qua giờ; mới nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + """
            where l.taiKhoanDat.id = :idTaiKhoan
              and (l.trangThai not in :trangThai or k.gioKetThuc <= :bayGio)
            order by k.gioBatDau desc, l.id desc
            """, countQuery = """
            select count(l) from LichHen l
            where l.taiKhoanDat.id = :idTaiKhoan
              and (l.trangThai not in :trangThai or l.khungGio.gioKetThuc <= :bayGio)
            """)
    Page<LichHen> timLichSuCuaTaiKhoan(Long idTaiKhoan, Collection<TrangThaiLichHen> trangThai, LocalDateTime bayGio,
            Pageable pageable);

    /** Mã phiếu khám đang lưu, để biết phiếu có tồn tại mà không tải cả lịch hẹn (so lại bằng {@code equals}). */
    @Query("select l.maTokenPhieuKham from LichHen l where l.maTokenPhieuKham = :maPhieuKham")
    Optional<String> timMaPhieuKham(String maPhieuKham);

    /**
     * Hồ sơ bệnh nhân đã có lịch hẹn (ở 1 trong các trạng thái cho trước, với bác sĩ bất kỳ) mà giờ khám dự kiến nằm
     * trong [tu, den)? Dùng để chặn 1 bệnh nhân giữ 2 lịch trùng khung giờ (BOOK-04).
     */
    @Query("""
            select count(l) > 0 from LichHen l
            where l.hoSoBenhNhan.id = :idHoSoBenhNhan
              and l.trangThai in :trangThai
              and l.khungGio.gioBatDau >= :tu
              and l.khungGio.gioBatDau < :den
            """)
    boolean coLichTrongKhoang(Long idHoSoBenhNhan, Collection<TrangThaiLichHen> trangThai, LocalDateTime tu,
            LocalDateTime den);

    /** Số lịch hẹn của hồ sơ ở các trạng thái cho trước, có giờ khám dự kiến từ {@code tu} trở đi. */
    @Query("""
            select count(l) from LichHen l
            where l.hoSoBenhNhan.id = :idHoSoBenhNhan
              and l.trangThai in :trangThai
              and l.khungGio.gioBatDau >= :tu
            """)
    long demLichCuaHoSoTu(Long idHoSoBenhNhan, Collection<TrangThaiLichHen> trangThai, LocalDateTime tu);

    /** Số lịch hẹn đặt bằng SĐT liên hệ này ở các trạng thái cho trước, có giờ khám dự kiến từ {@code tu} trở đi. */
    @Query("""
            select count(l) from LichHen l
            where l.soDienThoaiLienHe = :soDienThoai
              and l.trangThai in :trangThai
              and l.khungGio.gioBatDau >= :tu
            """)
    long demLichTheoSoDienThoaiTu(String soDienThoai, Collection<TrangThaiLichHen> trangThai, LocalDateTime tu);

}
