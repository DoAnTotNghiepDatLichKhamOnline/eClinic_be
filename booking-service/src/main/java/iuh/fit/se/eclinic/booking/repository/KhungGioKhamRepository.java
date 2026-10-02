package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
import jakarta.persistence.LockModeType;

public interface KhungGioKhamRepository extends JpaRepository<KhungGioKham, Long> {

    List<KhungGioKham> findByLichLamViecIdOrderByGioBatDauAsc(Long lichLamViecId);

    List<KhungGioKham> findByLichLamViecIdAndTrangThaiOrderByGioBatDauAsc(Long lichLamViecId,
            TrangThaiKhungGio trangThai);

    /** Các lượt khám của nhiều ca, bỏ trạng thái cho trước (thường là DA_HUY), theo giờ bắt đầu. */
    List<KhungGioKham> findByLichLamViecIdInAndTrangThaiNotOrderByGioBatDauAsc(Collection<Long> lichLamViecIds,
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

    /**
     * Khoá (SELECT ... FOR UPDATE) mọi lượt khám của 1 khung 1 giờ: các lượt của ca bắt đầu trong [tu, den), theo giờ
     * bắt đầu. Mọi request đặt cùng khung đều khoá các dòng này theo cùng thứ tự nên chạy lần lượt, không deadlock.
     * Phải gọi trong transaction đặt lịch.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select k from KhungGioKham k
            where k.lichLamViec.id = :idLichLamViec
              and k.gioBatDau >= :tu
              and k.gioBatDau < :den
            order by k.gioBatDau
            """)
    List<KhungGioKham> khoaCacLuotCuaKhung(Long idLichLamViec, LocalDateTime tu, LocalDateTime den);

    /**
     * Số lượt khám của phòng trong ngày đứng trước 1 lượt (theo giờ bắt đầu, rồi theo id). Đếm mọi dòng kể cả lượt đã
     * hủy, nên thứ hạng của 1 lượt không đổi theo thời gian: số thứ tự khám = kết quả + 1 (quy tắc #4, #12).
     */
    @Query("""
            select count(k) from KhungGioKham k
            where k.lichLamViec.phongKham.id = :idPhongKham
              and k.lichLamViec.ngayLamViec = :ngay
              and (k.gioBatDau < :gioBatDau or (k.gioBatDau = :gioBatDau and k.id < :id))
            """)
    long demLuotDungTruoc(Long idPhongKham, LocalDate ngay, LocalDateTime gioBatDau, Long id);

}
