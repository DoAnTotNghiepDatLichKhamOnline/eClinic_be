package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {

    /** Khoá dòng ca (SELECT ... FOR UPDATE) trước khi sửa / hủy ca hoặc duyệt yêu cầu của ca: các thao tác đó chạy lần lượt. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LichLamViec l where l.id = :id")
    Optional<LichLamViec> findByIdForUpdate(Long id);

    List<LichLamViec> findByBacSiIdAndNgayLamViecBetweenOrderByNgayLamViecAscGioBatDauAsc(Long bacSiId,
            LocalDate tuNgay, LocalDate denNgay);

    /**
     * Các ca đặt lịch được của 1 ngày: ca còn hoạt động, bác sĩ đang công tác VÀ tài khoản bác sĩ đã kích hoạt
     * (vô hiệu hoá tài khoản không đổi {@code bac_si.trang_thai}). Lấy sẵn bác sĩ, tài khoản, phòng khám.
     *
     * @param idBacSi      có giá trị = chỉ ca của bác sĩ này (bỏ qua idChuyenKhoa)
     * @param idChuyenKhoa dùng khi idBacSi null: ca của mọi bác sĩ thuộc chuyên khoa
     */
    @Query("""
            select l from LichLamViec l
              join fetch l.bacSi b
              join fetch b.taiKhoan t
              join fetch l.phongKham
            where l.ngayLamViec = :ngay
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and ((:idBacSi is not null and b.id = :idBacSi)
                or (:idBacSi is null and b.chuyenKhoa.id = :idChuyenKhoa))
            order by t.hoTen, b.id, l.gioBatDau
            """)
    List<LichLamViec> timCaDatLichDuoc(LocalDate ngay, Long idBacSi, Long idChuyenKhoa);

    /**
     * Ca theo id, lấy sẵn bác sĩ, tài khoản bác sĩ, phòng khám. Không lọc trạng thái: lúc đặt lịch service tự phân biệt
     * "không có ca" (404) với "ca không còn nhận đặt lịch" (điều kiện như {@link #timCaDatLichDuoc}).
     */
    @Query("""
            select l from LichLamViec l
              join fetch l.bacSi b
              join fetch b.taiKhoan
              join fetch l.phongKham
            where l.id = :id
            """)
    Optional<LichLamViec> timTheoIdKemBacSiVaPhongKham(Long id);

    /** Số lượt khám còn đặt được của 1 ngày có ca làm việc. */
    interface SoChoTheoNgay {

        LocalDate getNgay();

        Long getSoChoConLai();
    }

    /**
     * Mỗi ngày có ca đặt lịch được trong [tuNgay, denNgay] (điều kiện như {@link #timCaDatLichDuoc}) kèm số lượt
     * còn trống bắt đầu từ {@code moc} trở đi. Ngày đã hết chỗ vẫn có trong kết quả với số 0.
     */
    @Query("""
            select l.ngayLamViec as ngay,
                   sum(case when k.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio.CON_TRONG
                             and k.gioBatDau >= :moc then 1 else 0 end) as soChoConLai
            from KhungGioKham k
              join k.lichLamViec l
              join l.bacSi b
              join b.taiKhoan t
            where l.ngayLamViec between :tuNgay and :denNgay
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and ((:idBacSi is not null and b.id = :idBacSi)
                or (:idBacSi is null and b.chuyenKhoa.id = :idChuyenKhoa))
            group by l.ngayLamViec
            order by l.ngayLamViec
            """)
    List<SoChoTheoNgay> demChoTrongTheoNgay(LocalDate tuNgay, LocalDate denNgay, LocalDateTime moc, Long idBacSi,
            Long idChuyenKhoa);

    /**
     * Các ca đặt lịch được của 1 bác sĩ trong [tuNgay, denNgay], điều kiện như {@link #timCaDatLichDuoc}; theo ngày
     * rồi giờ bắt đầu. Lấy sẵn bác sĩ, tài khoản, phòng khám.
     */
    @Query("""
            select l from LichLamViec l
              join fetch l.bacSi b
              join fetch b.taiKhoan t
              join fetch l.phongKham
            where l.ngayLamViec between :tuNgay and :denNgay
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and b.id = :idBacSi
            order by l.ngayLamViec, l.gioBatDau
            """)
    List<LichLamViec> timCaDatLichDuocTrongKhoang(LocalDate tuNgay, LocalDate denNgay, Long idBacSi);

    /**
     * Lịch làm việc cho bác sĩ / quản trị viên: MỌI ca trong [tuNgay, denNgay] kể cả ca đã huỷ và ca của bác sĩ đã
     * ngừng công tác. Bộ lọc null = không lọc. Lấy sẵn bác sĩ, tài khoản, chuyên khoa, phòng khám.
     */
    @Query("""
            select l from LichLamViec l
              join fetch l.bacSi b
              join fetch b.taiKhoan t
              join fetch b.chuyenKhoa c
              join fetch l.phongKham p
            where l.ngayLamViec between :tuNgay and :denNgay
              and (:idChuyenKhoa is null or c.id = :idChuyenKhoa)
              and (:idBacSi is null or b.id = :idBacSi)
              and (:idPhongKham is null or p.id = :idPhongKham)
            order by l.ngayLamViec, l.gioBatDau, t.hoTen, l.id
            """)
    List<LichLamViec> timTrongKhoang(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi,
            Long idPhongKham);

    /** Số lượt khám của 1 ca: tổng (không tính lượt đã huỷ), đã đặt, còn trống. */
    interface SoLuotCuaCa {

        Long getIdLichLamViec();

        Long getTongSoLuot();

        Long getSoLuotDaDat();

        Long getSoLuotConTrong();
    }

    /** Đếm lượt khám của nhiều ca trong 1 câu query. Ca không còn lượt nào (mọi lượt đã huỷ) không có trong kết quả. */
    @Query("""
            select k.lichLamViec.id as idLichLamViec,
                   count(k) as tongSoLuot,
                   sum(case when k.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio.DA_DAT
                            then 1 else 0 end) as soLuotDaDat,
                   sum(case when k.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio.CON_TRONG
                            then 1 else 0 end) as soLuotConTrong
            from KhungGioKham k
            where k.lichLamViec.id in :idCacCa
              and k.trangThai <> iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio.DA_HUY
            group by k.lichLamViec.id
            """)
    List<SoLuotCuaCa> demLuotTheoCa(Collection<Long> idCacCa);

    /** Số lượt khám còn đặt được của 1 bác sĩ trong 1 ngày. */
    interface SoChoCuaBacSiTheoNgay {

        Long getIdBacSi();

        LocalDate getNgay();

        Long getSoChoConLai();
    }

    /**
     * Mỗi (bác sĩ, ngày) CÒN chỗ của 1 chuyên khoa trong [tuNgay, denNgay], điều kiện như {@link #demChoTrongTheoNgay}.
     * Sắp theo bác sĩ rồi theo ngày: dòng đầu tiên của mỗi bác sĩ là ngày còn chỗ sớm nhất.
     */
    @Query("""
            select b.id as idBacSi, l.ngayLamViec as ngay, count(k) as soChoConLai
            from KhungGioKham k
              join k.lichLamViec l
              join l.bacSi b
              join b.taiKhoan t
            where l.ngayLamViec between :tuNgay and :denNgay
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and b.chuyenKhoa.id = :idChuyenKhoa
              and k.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio.CON_TRONG
              and k.gioBatDau >= :moc
            group by b.id, l.ngayLamViec
            order by b.id, l.ngayLamViec
            """)
    List<SoChoCuaBacSiTheoNgay> demChoTrongTheoBacSiVaNgay(LocalDate tuNgay, LocalDate denNgay, LocalDateTime moc,
            Long idChuyenKhoa);

    /**
     * Bác sĩ đã có ca khác (còn hoạt động) chồng giờ trong cùng ngày? (2 khoảng [s1,e1) và [s2,e2) chồng nhau khi s1 < e2 và e1 > s2)
     *
     * @param excludeId id ca đang sửa (bỏ qua chính nó), truyền null khi tạo mới
     */
    @Query("""
            select count(l) > 0 from LichLamViec l
            where l.bacSi.id = :bacSiId
              and l.ngayLamViec = :ngayLamViec
              and l.gioBatDau < :gioKetThuc
              and l.gioKetThuc > :gioBatDau
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (:excludeId is null or l.id <> :excludeId)
            """)
    boolean existsTrungCaCuaBacSi(Long bacSiId, LocalDate ngayLamViec, LocalTime gioBatDau, LocalTime gioKetThuc,
            Long excludeId);

    /** Phòng khám đã được xếp ca khác (của bác sĩ bất kỳ, còn hoạt động) chồng giờ trong cùng ngày? */
    @Query("""
            select count(l) > 0 from LichLamViec l
            where l.phongKham.id = :phongKhamId
              and l.ngayLamViec = :ngayLamViec
              and l.gioBatDau < :gioKetThuc
              and l.gioKetThuc > :gioBatDau
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (:excludeId is null or l.id <> :excludeId)
            """)
    boolean existsTrungCaCuaPhongKham(Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeId);

    /** Id các ca còn hoạt động chưa bắt đầu của bác sĩ (ca đang diễn ra không tính), ca sớm nhất trước. */
    @Query("""
            select l.id from LichLamViec l
            where l.bacSi.id = :idBacSi
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (l.ngayLamViec > :homNay or (l.ngayLamViec = :homNay and l.gioBatDau > :gioHienTai))
            order by l.ngayLamViec, l.gioBatDau
            """)
    List<Long> timIdCaSapToiCuaBacSi(Long idBacSi, LocalDate homNay, LocalTime gioHienTai);

}
