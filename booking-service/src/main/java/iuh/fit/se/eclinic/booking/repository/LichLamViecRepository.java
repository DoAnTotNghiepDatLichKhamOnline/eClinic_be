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

    /** Khoá dòng ca (FOR UPDATE) để sửa, hủy ca và duyệt yêu cầu của ca chạy lần lượt. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LichLamViec l where l.id = :id")
    Optional<LichLamViec> findByIdForUpdate(Long id);

    List<LichLamViec> findByBacSiIdAndNgayLamViecBetweenOrderByNgayLamViecAscGioBatDauAsc(Long bacSiId,
            LocalDate tuNgay, LocalDate denNgay);

    /** Ca đặt lịch được trong 1 ngày: ca hoạt động, bác sĩ công tác và tài khoản bác sĩ còn kích hoạt. */
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

    /** Ca theo id, lấy sẵn bác sĩ, tài khoản bác sĩ, phòng khám. */
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

    /** Mỗi ngày có ca đặt lịch được trong khoảng, kèm số lượt còn trống từ {@code moc}. */
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

    /** Ca đặt lịch được của 1 bác sĩ trong khoảng ngày, theo ngày rồi giờ bắt đầu. */
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

    /** Mọi ca trong khoảng ngày, kể cả ca đã hủy và ca của bác sĩ đã ngừng công tác. */
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

    /** Mỗi (bác sĩ, ngày) còn chỗ của 1 chuyên khoa trong khoảng ngày. */
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

    /** Bác sĩ đã có ca khác còn hoạt động chồng giờ trong cùng ngày? */
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

    /** Ca nguồn của 1 lần sao chép: ca CÒN HOẠT ĐỘNG có ngày trong [tuNgay, denNgay], theo ngày, giờ bắt đầu. */
    @Query("""
            select l from LichLamViec l
            where l.ngayLamViec between :tuNgay and :denNgay
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (:idChuyenKhoa is null or l.bacSi.chuyenKhoa.id = :idChuyenKhoa)
            order by l.ngayLamViec, l.gioBatDau, l.id
            """)
    List<LichLamViec> timCaNguonSaoChep(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa);

    /** Bác sĩ, ngày và giờ của 1 ca, đủ để cộng giờ làm theo tuần. */
    interface GioCuaCa {

        Long getIdBacSi();

        LocalDate getNgay();

        LocalTime getGioBatDau();

        LocalTime getGioKetThuc();
    }

    /** Giờ các ca CÒN HOẠT ĐỘNG có ngày trong [tuNgay, denNgay] (kể cả ca đã qua). */
    @Query("""
            select l.bacSi.id as idBacSi, l.ngayLamViec as ngay, l.gioBatDau as gioBatDau, l.gioKetThuc as gioKetThuc
            from LichLamViec l
            where l.ngayLamViec between :tuNgay and :denNgay
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (:idBacSi is null or l.bacSi.id = :idBacSi)
              and (:idChuyenKhoa is null or l.bacSi.chuyenKhoa.id = :idChuyenKhoa)
              and (:excludeId is null or l.id <> :excludeId)
            """)
    List<GioCuaCa> timGioCaHoatDong(LocalDate tuNgay, LocalDate denNgay, Long idBacSi, Long idChuyenKhoa,
            Long excludeId);

    /** Id các ca CÒN HOẠT ĐỘNG vào các ngày đã cho, theo id (thứ tự khoá như duyệt nhóm yêu cầu). */
    @Query("""
            select l.id from LichLamViec l
            where l.ngayLamViec in :cacNgay
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
            order by l.id
            """)
    List<Long> timIdCaHoatDongTrongCacNgay(Collection<LocalDate> cacNgay);

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
