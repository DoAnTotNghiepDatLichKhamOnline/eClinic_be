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
            left join fetch l.nguoiGiamHo g
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

    /** Điều kiện "lịch hẹn của tài khoản" dùng chung cho 3 truy vấn bên dưới; {@code g} là người giám hộ (left join). */
    String CUA_TAI_KHOAN = "(l.taiKhoanDat.id = :idTaiKhoan or l.hoSoBenhNhan.id = :idHoSoCuaToi or g.cccd = :cccdCuaToi)";

    String DEM_LICH_HEN = "select count(l) from LichHen l left join l.nguoiGiamHo g ";

    /**
     * Lọc theo người khám, nối sau {@link #CUA_TAI_KHOAN}: {@code chiBanThan} = chỉ lịch của hồ sơ đã liên kết của tài
     * khoản (tài khoản chưa có hồ sơ thì không có dòng nào); {@code chiNguoiKhac} = chỉ lịch của hồ sơ khác (tài khoản
     * chưa có hồ sơ thì là mọi lịch). Cả hai false = không lọc.
     */
    String THEO_NGUOI_KHAM = " and (:chiBanThan = false or l.hoSoBenhNhan.id = :idHoSoCuaToi)"
            + " and (:chiNguoiKhac = false or l.hoSoBenhNhan.id <> coalesce(:idHoSoCuaToi, 0L)) ";

    /**
     * BOOK-07: lịch hẹn của tài khoản, giờ khám muộn nhất trước. Gồm lịch tài khoản đặt khi đã đăng nhập (cho bản thân
     * hoặc người thân), lịch của hồ sơ bệnh nhân đã liên kết với tài khoản (kể cả lịch đặt như khách trước khi có tài
     * khoản, quy tắc #3) và lịch của người khám mà người giám hộ khai đúng số CCCD của hồ sơ đó.
     * {@code idHoSoCuaToi}, {@code cccdCuaToi}: null nếu tài khoản chưa có hồ sơ ở trạng thái DA_LIEN_KET (so sánh với
     * null không khớp dòng nào). Thứ tự viết trong JPQL nên {@code pageable} không kèm Sort.
     */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM
            + " order by k.gioBatDau desc, l.id desc",
            countQuery = DEM_LICH_HEN + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM)
    Page<LichHen> timCuaTaiKhoan(Long idTaiKhoan, Long idHoSoCuaToi, String cccdCuaToi, boolean chiBanThan,
            boolean chiNguoiKhac, Pageable pageable);

    /**
     * Lịch hẹn (mọi trạng thái) của tài khoản có giờ khám dự kiến trong [tu, den), theo giờ khám. Cho màn hình lịch của
     * bệnh nhân; cùng điều kiện và bộ lọc người khám với {@link #timCuaTaiKhoan}.
     */
    @Query(LICH_HEN_KEM_CHI_TIET + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM + """
              and k.gioBatDau >= :tu
              and k.gioBatDau < :den
            order by k.gioBatDau asc, l.id asc
            """)
    List<LichHen> timCuaTaiKhoanTrongKhoang(Long idTaiKhoan, Long idHoSoCuaToi, String cccdCuaToi,
            boolean chiBanThan, boolean chiNguoiKhac, LocalDateTime tu, LocalDateTime den);

    /**
     * 1 lịch hẹn mà tài khoản được xem (cùng điều kiện với {@link #timCuaTaiKhoan}) theo mã phiếu khám. Cột mã dùng
     * collation không phân biệt hoa thường nên nơi gọi phải so lại chính xác bằng {@code equals}.
     */
    @Query(LICH_HEN_KEM_CHI_TIET + "where " + CUA_TAI_KHOAN + " and l.maTokenPhieuKham = :maPhieuKham")
    Optional<LichHen> timCuaTaiKhoanTheoMaPhieuKham(Long idTaiKhoan, Long idHoSoCuaToi, String cccdCuaToi,
            String maPhieuKham);

    /** Lịch sắp tới của tài khoản: còn hiệu lực ({@code trangThai}) và lượt khám chưa kết thúc; gần nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM + """
              and l.trangThai in :trangThai
              and k.gioKetThuc > :bayGio
            order by k.gioBatDau asc, l.id asc
            """, countQuery = DEM_LICH_HEN + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM + """
              and l.trangThai in :trangThai
              and l.khungGio.gioKetThuc > :bayGio
            """)
    Page<LichHen> timSapToiCuaTaiKhoan(Long idTaiKhoan, Long idHoSoCuaToi, String cccdCuaToi, boolean chiBanThan,
            boolean chiNguoiKhac, Collection<TrangThaiLichHen> trangThai, LocalDateTime bayGio, Pageable pageable);

    /** Phần còn lại của {@link #timSapToiCuaTaiKhoan}: lịch đã khám, đã hủy, bị từ chối hoặc đã qua giờ; mới nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM + """
              and (l.trangThai not in :trangThai or k.gioKetThuc <= :bayGio)
            order by k.gioBatDau desc, l.id desc
            """, countQuery = DEM_LICH_HEN + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM + """
              and (l.trangThai not in :trangThai or l.khungGio.gioKetThuc <= :bayGio)
            """)
    Page<LichHen> timLichSuCuaTaiKhoan(Long idTaiKhoan, Long idHoSoCuaToi, String cccdCuaToi, boolean chiBanThan,
            boolean chiNguoiKhac, Collection<TrangThaiLichHen> trangThai, LocalDateTime bayGio, Pageable pageable);

    /**
     * Điều kiện "tài khoản được xem kết quả khám": người khám là chủ tài khoản (hồ sơ đã liên kết, bất kể ai đặt) hoặc
     * chính tài khoản này đã đặt lịch. Lịch tài khoản chỉ thấy vì là người giám hộ theo CCCD thì KHÔNG được xem kết quả.
     */
    String DUOC_XEM_KET_QUA = "(l.hoSoBenhNhan.id = :idHoSoCuaToi or l.taiKhoanDat.id = :idTaiKhoan)";

    /**
     * Các lượt đã khám xong mà tài khoản được xem kết quả, giờ khám muộn nhất trước. {@code idHoSoCuaToi} null nếu tài
     * khoản chưa có hồ sơ ở trạng thái DA_LIEN_KET. Thứ tự viết trong JPQL nên {@code pageable} không kèm Sort.
     */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where l.trangThai = :daKham and " + DUOC_XEM_KET_QUA + THEO_NGUOI_KHAM
            + " order by k.gioBatDau desc, l.id desc",
            countQuery = "select count(l) from LichHen l where l.trangThai = :daKham and " + DUOC_XEM_KET_QUA
                    + THEO_NGUOI_KHAM)
    Page<LichHen> timDaKhamDuocXemKetQua(Long idTaiKhoan, Long idHoSoCuaToi, TrangThaiLichHen daKham,
            boolean chiBanThan, boolean chiNguoiKhac, Pageable pageable);

    long countByHoSoBenhNhanId(Long hoSoBenhNhanId);

    /**
     * Lịch hẹn (mọi trạng thái) của 1 bác sĩ có giờ khám dự kiến trong [tu, den), theo giờ khám. Cho bác sĩ xem lịch
     * hẹn trong ngày của mình.
     */
    @Query(LICH_HEN_KEM_CHI_TIET + """
            where b.id = :idBacSi
              and k.gioBatDau >= :tu
              and k.gioBatDau < :den
            order by k.gioBatDau asc, l.id asc
            """)
    List<LichHen> timCuaBacSiTrongKhoang(Long idBacSi, LocalDateTime tu, LocalDateTime den);

    /** Lịch hẹn (mọi trạng thái) của 1 ca làm việc, theo giờ khám. Cho quản trị viên. */
    @Query(LICH_HEN_KEM_CHI_TIET + """
            where k.lichLamViec.id = :idLichLamViec
            order by k.gioBatDau asc, l.id asc
            """)
    List<LichHen> timTheoCa(Long idLichLamViec);

    /** 1 lịch hẹn theo id, kèm mọi thứ danh sách của bác sĩ / quản trị viên hiển thị. */
    @Query(LICH_HEN_KEM_CHI_TIET + "where l.id = :id")
    Optional<LichHen> timTheoIdKemChiTiet(Long id);

    /**
     * Các lịch hẹn (mọi trạng thái, mọi bác sĩ) của 1 hồ sơ bệnh nhân có giờ khám dự kiến trước {@code truoc}, mới nhất
     * trước. Cho bác sĩ xem các lần khám trước của bệnh nhân.
     */
    @Query(LICH_HEN_KEM_CHI_TIET + """
            where l.hoSoBenhNhan.id = :idHoSoBenhNhan
              and k.gioBatDau < :truoc
            order by k.gioBatDau desc, l.id desc
            """)
    List<LichHen> timCuaHoSoTruoc(Long idHoSoBenhNhan, LocalDateTime truoc);

    boolean existsByMaTraCuu(String maTraCuu);

    /** Lịch hẹn theo mã tra cứu ngắn, kèm mọi thứ danh sách của bác sĩ / quản trị viên hiển thị. */
    @Query(LICH_HEN_KEM_CHI_TIET + "where l.maTraCuu = :maTraCuu")
    Optional<LichHen> timTheoMaTraCuu(String maTraCuu);

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
