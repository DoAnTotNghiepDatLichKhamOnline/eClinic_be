package iuh.fit.se.eclinic.booking.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

/** Trạng thái nhắc lịch là 2 cột nhac_moc_* của lịch hẹn; chỉ ghi bằng câu UPDATE riêng này. */
public interface NhacLichHenRepository extends Repository<LichHen, Long> {

    /**
     * Ghi nhận đã xử lý mốc {@code soPhut} (mốc này nhỏ hơn mọi mốc đã xử lý trước đó); {@code daGui} = đã gửi nhắc ở mốc
     * này. Không tăng version, không đổi ngay_cap_nhat của lịch hẹn.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            update lich_hen
            set nhac_moc_da_xu_ly = :soPhut,
                nhac_moc_da_gui = case when :daGui then :soPhut else nhac_moc_da_gui end
            where id_lich_hen = :idLichHen
            """, nativeQuery = true)
    int ghiMoc(Long idLichHen, int soPhut, boolean daGui);

}
