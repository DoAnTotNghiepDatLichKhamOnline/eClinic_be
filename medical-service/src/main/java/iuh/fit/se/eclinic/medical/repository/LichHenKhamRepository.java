package iuh.fit.se.eclinic.medical.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import jakarta.persistence.LockModeType;

/**
 * Lịch hẹn (bảng của booking-service) cho việc khám bệnh. Kế thừa {@link Repository} nên không có save / delete.
 * <p>
 * NGOẠI LỆ DUY NHẤT của quy ước "service chỉ ghi bảng của mình" (docs/QUY-UOC-CODE.md mục 7): khi bác sĩ ghi nhận kết
 * quả khám, medical-service đổi {@code trangThai} của lịch hẹn sang DA_HOAN_THANH trong CÙNG transaction với việc lưu
 * hồ sơ bệnh án, để không bao giờ có bệnh án mà lịch hẹn chưa hoàn thành (hoặc ngược lại). Qua repository này CHỈ được
 * đổi {@code trangThai} của dòng lấy bằng {@link #findByIdForUpdate}; mọi trường khác của lịch hẹn vẫn do
 * booking-service ghi.
 */
public interface LichHenKhamRepository extends Repository<LichHen, Long> {

    Optional<LichHen> findById(Long id);

    /** Khoá dòng lịch hẹn: 2 lần ghi nhận cùng lúc chạy lần lượt, lần sau thấy lịch hẹn đã hoàn thành. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LichHen l where l.id = :id")
    Optional<LichHen> findByIdForUpdate(Long id);

    /**
     * Bác sĩ có lịch hẹn với hồ sơ bệnh nhân này mà giờ khám từ {@code gioKham} trở đi không: khi đó lượt khám lúc
     * {@code gioKham} nằm trong hồ sơ khám bác sĩ mở được (lượt khám của chính bác sĩ cũng thoả).
     */
    @Query("""
            select count(l) > 0 from LichHen l
            where l.bacSi.id = :idBacSi and l.hoSoBenhNhan.id = :idHoSoBenhNhan and l.khungGio.gioBatDau >= :gioKham
            """)
    boolean bacSiXemDuocLuotKham(Long idBacSi, Long idHoSoBenhNhan, LocalDateTime gioKham);

}
