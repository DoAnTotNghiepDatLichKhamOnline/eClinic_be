package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {

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

}
