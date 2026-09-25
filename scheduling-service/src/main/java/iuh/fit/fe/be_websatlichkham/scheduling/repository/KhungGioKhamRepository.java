package iuh.fit.fe.be_websatlichkham.scheduling.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.KhungGioKham;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiKhungGio;
import jakarta.persistence.LockModeType;

public interface KhungGioKhamRepository extends JpaRepository<KhungGioKham, Long> {

    List<KhungGioKham> findByLichLamViecIdOrderByGioBatDauAsc(Long lichLamViecId);

    List<KhungGioKham> findByLichLamViecIdAndTrangThaiOrderByGioBatDauAsc(Long lichLamViecId,
            TrangThaiKhungGio trangThai);

    /** Các khung giờ của bác sĩ có trạng thái cho trước, bắt đầu trong khoảng [tu, den). */
    @Query("""
            select k from KhungGioKham k
            where k.lichLamViec.bacSi.id = :bacSiId
              and k.trangThai = :trangThai
              and k.gioBatDau >= :tu
              and k.gioBatDau < :den
            order by k.gioBatDau
            """)
    List<KhungGioKham> findByBacSiAndTrangThaiInRange(Long bacSiId, TrangThaiKhungGio trangThai, LocalDateTime tu,
            LocalDateTime den);

    /**
     * Khoá khung giờ (SELECT ... FOR UPDATE) trong transaction đặt lịch, để 2 request đặt cùng khung giờ chạy tuần tự.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from KhungGioKham k where k.id = :id")
    Optional<KhungGioKham> findByIdForUpdate(Long id);

}
