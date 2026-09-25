package iuh.fit.fe.be_websatlichkham.scheduling.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.LichLamViec;

public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {

    List<LichLamViec> findByBacSiIdAndNgayLamViecBetweenOrderByNgayLamViecAscGioBatDauAsc(Long bacSiId,
            LocalDate tuNgay, LocalDate denNgay);

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
              and l.trangThai = iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLichLamViec.HOAT_DONG
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
              and l.trangThai = iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (:excludeId is null or l.id <> :excludeId)
            """)
    boolean existsTrungCaCuaPhongKham(Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeId);

}
