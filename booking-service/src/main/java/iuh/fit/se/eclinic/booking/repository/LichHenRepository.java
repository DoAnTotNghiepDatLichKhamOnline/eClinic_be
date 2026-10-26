package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiHangDoi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import jakarta.persistence.LockModeType;

public interface LichHenRepository extends JpaRepository<LichHen, Long> {

    /** Lịch hẹn kèm mọi thứ phiếu khám hiển thị; quan hệ đều là *-to-one nên phân trang được. */
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

    /** Lịch hẹn theo mã phiếu khám, kèm mọi thứ phiếu khám hiển thị. */
    @Query(LICH_HEN_KEM_CHI_TIET + "where l.maTokenPhieuKham = :maPhieuKham")
    Optional<LichHen> timTheoMaPhieuKham(String maPhieuKham);

    /** Điều kiện "lịch hẹn của tài khoản" dùng chung cho 3 truy vấn bên dưới; {@code g} là người giám hộ. */
    String CUA_TAI_KHOAN = "(l.taiKhoanDat.id = :idTaiKhoan or l.hoSoBenhNhan.id = :idHoSoCuaToi or g.cccd = :cccdCuaToi)";

    String DEM_LICH_HEN = "select count(l) from LichHen l left join l.nguoiGiamHo g ";

    /** Lọc theo người khám, nối sau {@link #CUA_TAI_KHOAN}: chỉ bản thân hoặc chỉ người khác. */
    String THEO_NGUOI_KHAM = " and (:chiBanThan = false or l.hoSoBenhNhan.id = :idHoSoCuaToi)"
            + " and (:chiNguoiKhac = false or l.hoSoBenhNhan.id <> coalesce(:idHoSoCuaToi, 0L)) ";

    /** Lịch hẹn của tài khoản, giờ khám muộn nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM
            + " order by k.gioBatDau desc, l.id desc",
            countQuery = DEM_LICH_HEN + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM)
    Page<LichHen> timCuaTaiKhoan(Long idTaiKhoan, Long idHoSoCuaToi, String cccdCuaToi, boolean chiBanThan,
            boolean chiNguoiKhac, Pageable pageable);

    /** Lịch hẹn (mọi trạng thái) của tài khoản có giờ khám dự kiến trong [tu, den), theo giờ khám. */
    @Query(LICH_HEN_KEM_CHI_TIET + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM + """
              and k.gioBatDau >= :tu
              and k.gioBatDau < :den
            order by k.gioBatDau asc, l.id asc
            """)
    List<LichHen> timCuaTaiKhoanTrongKhoang(Long idTaiKhoan, Long idHoSoCuaToi, String cccdCuaToi,
            boolean chiBanThan, boolean chiNguoiKhac, LocalDateTime tu, LocalDateTime den);

    /** 1 lịch hẹn mà tài khoản được xem (cùng điều kiện với {@link #timCuaTaiKhoan}) theo mã phiếu khám. */
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

    /** Phần còn lại của {@link #timSapToiCuaTaiKhoan}: lịch đã khám, đã hủy hoặc đã qua; mới nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM + """
              and (l.trangThai not in :trangThai or k.gioKetThuc <= :bayGio)
            order by k.gioBatDau desc, l.id desc
            """, countQuery = DEM_LICH_HEN + "where " + CUA_TAI_KHOAN + THEO_NGUOI_KHAM + """
              and (l.trangThai not in :trangThai or l.khungGio.gioKetThuc <= :bayGio)
            """)
    Page<LichHen> timLichSuCuaTaiKhoan(Long idTaiKhoan, Long idHoSoCuaToi, String cccdCuaToi, boolean chiBanThan,
            boolean chiNguoiKhac, Collection<TrangThaiLichHen> trangThai, LocalDateTime bayGio, Pageable pageable);

    /** Điều kiện "được xem kết quả khám": người khám là chủ tài khoản hoặc tài khoản đã đặt lịch. */
    String DUOC_XEM_KET_QUA = "(l.hoSoBenhNhan.id = :idHoSoCuaToi or l.taiKhoanDat.id = :idTaiKhoan)";

    /** Các lượt đã khám xong mà tài khoản được xem kết quả, giờ khám muộn nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where l.trangThai = :daKham and " + DUOC_XEM_KET_QUA + THEO_NGUOI_KHAM
            + " order by k.gioBatDau desc, l.id desc",
            countQuery = "select count(l) from LichHen l where l.trangThai = :daKham and " + DUOC_XEM_KET_QUA
                    + THEO_NGUOI_KHAM)
    Page<LichHen> timDaKhamDuocXemKetQua(Long idTaiKhoan, Long idHoSoCuaToi, TrangThaiLichHen daKham,
            boolean chiBanThan, boolean chiNguoiKhac, Pageable pageable);

    long countByHoSoBenhNhanId(Long hoSoBenhNhanId);

    interface SoLichHenTheoHoSo {

        Long getIdHoSo();

        long getSoLichHen();
    }

    /** Số lịch hẹn (mọi trạng thái) của từng hồ sơ trong danh sách; hồ sơ chưa có lịch hẹn nào không có dòng. */
    @Query("""
            select l.hoSoBenhNhan.id as idHoSo, count(l) as soLichHen from LichHen l
            where l.hoSoBenhNhan.id in :idCacHoSo
            group by l.hoSoBenhNhan.id
            """)
    List<SoLichHenTheoHoSo> demTheoHoSo(Collection<Long> idCacHoSo);

    /** Bộ lọc của {@link #timChoQuanTri}; {@code h} là hồ sơ bệnh nhân, {@code g} người giám hộ (left join). */
    String LOC_QUAN_TRI = """
            where k.gioBatDau >= :tu and k.gioBatDau < :den
              and (:idChuyenKhoa is null or b.chuyenKhoa.id = :idChuyenKhoa)
              and (:idBacSi is null or b.id = :idBacSi)
              and (:idPhongKham is null or l.phongKham.id = :idPhongKham)
              and (:trangThai is null or l.trangThai = :trangThai)
              and (:kenhDat is null or l.kenhDat = :kenhDat)
              and (:canDoiLich is null or l.canDoiLich = :canDoiLich)
              and (:canDoiChieu is null or l.canDoiChieu = :canDoiChieu)
              and (:ma is null or l.maTraCuu = :ma)
              and (:cccd is null or h.cccd = :cccd or g.cccd = :cccd)
              and (:mau is null or l.hoTenDaNhap like :mau escape '!' or h.hoTen like :mau escape '!'
                   or l.soDienThoaiLienHe like :mau escape '!' or g.soDienThoai like :mau escape '!'
                   or l.emailLienHe like :mau escape '!')
            """;

    /** Lịch hẹn toàn viện cho quản trị viên, giờ khám trong [tu, den), sớm nhất trước. */
    @Query(value = """
            select l from LichHen l
            join fetch l.hoSoBenhNhan h
            left join fetch l.nguoiGiamHo g
            join fetch l.bacSi b
            join fetch b.taiKhoan
            join fetch b.chuyenKhoa
            join fetch l.phongKham
            join fetch l.khungGio k
            join fetch k.lichLamViec
            """ + LOC_QUAN_TRI + " order by k.gioBatDau asc, l.id asc", countQuery = """
            select count(l) from LichHen l
            join l.hoSoBenhNhan h
            left join l.nguoiGiamHo g
            join l.bacSi b
            join l.khungGio k
            """ + LOC_QUAN_TRI)
    Page<LichHen> timChoQuanTri(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa, Long idBacSi,
            Long idPhongKham, TrangThaiLichHen trangThai, KenhDatLich kenhDat, Boolean canDoiLich,
            Boolean canDoiChieu, String ma, String cccd, String mau, Pageable pageable);

    /** Lịch hẹn của 1 bác sĩ mà người khám hoặc người giám hộ có CCCD này, mới nhất trước. */
    @Query(LICH_HEN_KEM_CHI_TIET + """
            where b.id = :idBacSi
              and (l.hoSoBenhNhan.cccd = :cccd or g.cccd = :cccd)
            order by k.gioBatDau desc, l.id desc
            """)
    List<LichHen> timCuaBacSiTheoCccd(Long idBacSi, String cccd, Limit limit);

    /** Lịch hẹn tạo ra khi đổi lịch hẹn này (mỗi lịch cũ nhiều nhất 1 lịch mới). */
    Optional<LichHen> findByLichHenCuId(Long idLichHenCu);

    /** Lịch hẹn (mọi trạng thái) của 1 hồ sơ bệnh nhân cho quản trị viên, giờ khám muộn nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + "where l.hoSoBenhNhan.id = :idHoSoBenhNhan"
            + " order by k.gioBatDau desc, l.id desc",
            countQuery = "select count(l) from LichHen l where l.hoSoBenhNhan.id = :idHoSoBenhNhan")
    Page<LichHen> timCuaHoSo(Long idHoSoBenhNhan, Pageable pageable);

    /** Lịch hẹn (mọi trạng thái) của 1 bác sĩ có giờ khám dự kiến trong [tu, den), theo giờ khám. */
    @Query(LICH_HEN_KEM_CHI_TIET + """
            where b.id = :idBacSi
              and k.gioBatDau >= :tu
              and k.gioBatDau < :den
            order by k.gioBatDau asc, l.id asc
            """)
    List<LichHen> timCuaBacSiTrongKhoang(Long idBacSi, LocalDateTime tu, LocalDateTime den);

    /** Id lịch hẹn của 1 ca đang chờ gọi vào khám, số thứ tự nhỏ nhất trước (hàng đợi khám). Đọc không khoá. */
    @Query("""
            select l.id from LichHen l
            where l.khungGio.lichLamViec.id = :idLichLamViec
              and l.trangThaiHangDoi = :choKham
              and l.trangThai in :trangThai
              and l.canDoiLich = false
            order by l.soThuTu asc, l.id asc
            """)
    List<Long> timIdChoKhamCuaCa(Long idLichLamViec, TrangThaiHangDoi choKham, Collection<TrangThaiLichHen> trangThai,
            Limit limit);

    /** Lịch hẹn khác (trừ {@code idTru}) của bác sĩ đang khám mà chưa ghi nhận kết quả, giờ khám trong [tu, den). */
    @Query("""
            select l from LichHen l
            where l.bacSi.id = :idBacSi
              and l.trangThaiHangDoi = :dangKham
              and l.trangThai in :trangThai
              and l.canDoiLich = false
              and l.khungGio.gioBatDau >= :tu
              and l.khungGio.gioBatDau < :den
              and l.id <> :idTru
            """)
    List<LichHen> timDangKhamCuaBacSi(Long idBacSi, TrangThaiHangDoi dangKham, Collection<TrangThaiLichHen> trangThai,
            LocalDateTime tu, LocalDateTime den, Long idTru);

    /** Id các lịch hẹn của 1 ca đang ở 1 trong các trạng thái cho trước. Đọc không khoá. */
    @Query("select l.id from LichHen l where l.khungGio.lichLamViec.id = :idLichLamViec and l.trangThai in :trangThai")
    List<Long> timIdTheoCaVaTrangThai(Long idLichLamViec, Collection<TrangThaiLichHen> trangThai);

    /** Khoá (SELECT ... */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LichHen l where l.id in :cacId order by l.id")
    List<LichHen> khoaTheoId(Collection<Long> cacId);

    /** Lịch hẹn còn hiệu lực đang chờ đổi lịch vì ca bị hủy, giờ khám cũ sớm nhất trước. */
    @Query(value = LICH_HEN_KEM_CHI_TIET + """
            where l.canDoiLich = true and l.trangThai in :trangThai
            order by k.gioBatDau asc, l.id asc
            """, countQuery = "select count(l) from LichHen l where l.canDoiLich = true and l.trangThai in :trangThai")
    Page<LichHen> timCanDoiLich(Collection<TrangThaiLichHen> trangThai, Pageable pageable);

    /** Lịch hẹn theo trạng thái của 1 phòng trong 1 ngày, kèm lượt khám, để đánh lại số thứ tự. */
    @Query("""
            select l from LichHen l join fetch l.khungGio k
            where l.phongKham.id = :idPhongKham
              and k.lichLamViec.ngayLamViec = :ngay
              and l.trangThai in :trangThai
            """)
    List<LichHen> timCuaPhongTrongNgay(Long idPhongKham, LocalDate ngay, Collection<TrangThaiLichHen> trangThai);

    /** Lịch hẹn (mọi trạng thái) của 1 ca làm việc, theo giờ khám. Cho quản trị viên. */
    @Query(LICH_HEN_KEM_CHI_TIET + """
            where k.lichLamViec.id = :idLichLamViec
            order by k.gioBatDau asc, l.id asc
            """)
    List<LichHen> timTheoCa(Long idLichLamViec);

    /** 1 lịch hẹn theo id, kèm mọi thứ danh sách của bác sĩ / quản trị viên hiển thị. */
    @Query(LICH_HEN_KEM_CHI_TIET + "where l.id = :id")
    Optional<LichHen> timTheoIdKemChiTiet(Long id);

    /** Khoá dòng lịch hẹn (SELECT ... */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LichHen l where l.id = :id")
    Optional<LichHen> findByIdForUpdate(Long id);

    /** Như {@link #findByIdForUpdate}, theo mã phiếu khám; nơi gọi phải so lại mã chính xác bằng {@code equals}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LichHen l where l.maTokenPhieuKham = :maPhieuKham")
    Optional<LichHen> findByMaPhieuKhamForUpdate(String maPhieuKham);

    // ----- Dashboard: các câu đếm, định nghĩa từng số ở TongQuanBacSiResponse / TongQuanQuanTriResponse -----

    /** Lịch hẹn của bác sĩ ở 1 trạng thái, không cần đổi lịch, lượt khám bắt đầu sau {@code bayGio}. */
    @Query("""
            select count(l) from LichHen l
            where l.bacSi.id = :idBacSi
              and l.trangThai = :trangThai
              and l.canDoiLich = false
              and l.khungGio.gioBatDau > :bayGio
            """)
    long demSapToiCuaBacSi(Long idBacSi, TrangThaiLichHen trangThai, LocalDateTime bayGio);

    /** Số lịch hẹn của bác sĩ có giờ khám trong [tu, den), trạng thái KHÔNG thuộc {@code trangThai}. */
    @Query("""
            select count(l) from LichHen l
            where l.bacSi.id = :idBacSi
              and l.khungGio.gioBatDau >= :tu
              and l.khungGio.gioBatDau < :den
              and l.trangThai not in :trangThai
            """)
    long demCuaBacSiTrongKhoangKhongThuoc(Long idBacSi, LocalDateTime tu, LocalDateTime den,
            Collection<TrangThaiLichHen> trangThai);

    /** Số lịch hẹn của bác sĩ có giờ khám trong [tu, den), trạng thái thuộc {@code trangThai}. */
    @Query("""
            select count(l) from LichHen l
            where l.bacSi.id = :idBacSi
              and l.khungGio.gioBatDau >= :tu
              and l.khungGio.gioBatDau < :den
              and l.trangThai in :trangThai
            """)
    long demCuaBacSiTrongKhoangThuoc(Long idBacSi, LocalDateTime tu, LocalDateTime den,
            Collection<TrangThaiLichHen> trangThai);

    /** Như {@link #demCuaBacSiTrongKhoangThuoc} nhưng bỏ lịch hẹn đang chờ đổi lịch (ca đã bị hủy, sẽ không khám). */
    @Query("""
            select count(l) from LichHen l
            where l.bacSi.id = :idBacSi
              and l.khungGio.gioBatDau >= :tu
              and l.khungGio.gioBatDau < :den
              and l.trangThai in :trangThai
              and l.canDoiLich = false
            """)
    long demChuaKhamCuaBacSiTrongKhoang(Long idBacSi, LocalDateTime tu, LocalDateTime den,
            Collection<TrangThaiLichHen> trangThai);

    /** Số lịch hẹn được tạo từ {@code tu} trở đi, trừ 1 trạng thái (lịch cũ của 1 lần đổi lịch). */
    @Query("select count(l) from LichHen l where l.ngayTao >= :tu and l.trangThai <> :khongTinh")
    long demDaTaoTu(LocalDateTime tu, TrangThaiLichHen khongTinh);

    /** Lịch hẹn của 1 hồ sơ bệnh nhân có giờ khám trước {@code truoc}, mới nhất trước. */
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

    /** Hồ sơ bệnh nhân đã có lịch hẹn ở các trạng thái này với giờ khám trong [tu, den)? */
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

    /** Id lịch hẹn đến hạn nhắc ở 1 mốc: còn hiệu lực, lượt khám trong ({@code tu}, {@code den}], chưa nhắc. */
    @Query("""
            select l.id from LichHen l join l.khungGio k
            where l.trangThai in :trangThai and l.canDoiLich = false
              and k.gioBatDau > :tu and k.gioBatDau <= :den
              and not exists (select 1 from NhacLichHen n where n.idLichHen = l.id and n.soPhutTruoc = :soPhutTruoc)
            order by k.gioBatDau asc, l.id asc
            """)
    List<Long> timIdCanNhac(Collection<TrangThaiLichHen> trangThai, LocalDateTime tu, LocalDateTime den,
            int soPhutTruoc, Limit gioiHan);

    /** Hồ sơ đã có lịch hẹn còn hiệu lực từ {@code tu} với bác sĩ thuộc chuyên khoa này không. */
    @Query("""
            select count(l) > 0 from LichHen l join l.khungGio k
            where l.hoSoBenhNhan.id = :idHoSoBenhNhan and l.trangThai in :trangThai
              and k.gioBatDau >= :tu and l.bacSi.chuyenKhoa.id = :idChuyenKhoa
            """)
    boolean coLichSapToi(Long idHoSoBenhNhan, Collection<TrangThaiLichHen> trangThai, LocalDateTime tu,
            Long idChuyenKhoa);

    /** Lịch còn hiệu lực có lượt khám trước {@code moc} mà bệnh nhân chưa khám chuyển sang {@code khongDen}. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update versioned LichHen l set l.trangThai = :khongDen
            where l.trangThai = :conHieuLuc and l.canDoiLich = false and l.trangThaiHangDoi <> :dangKham
              and l.khungGio.id in (select k.id from KhungGioKham k where k.gioBatDau < :moc)
            """)
    int dongKhongDen(TrangThaiLichHen conHieuLuc, TrangThaiLichHen khongDen, TrangThaiHangDoi dangKham,
            LocalDateTime moc);

}
