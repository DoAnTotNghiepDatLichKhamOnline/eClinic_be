package iuh.fit.se.eclinic.booking.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

/**
 * CHỈ ĐỌC hồ sơ bệnh án (bảng của medical-service) để bác sĩ xem kết quả các lần khám trước trong hồ sơ khám của bệnh
 * nhân. Kế thừa {@link Repository} chứ không phải JpaRepository nên không có save / delete.
 */
public interface HoSoBenhAnChiDocRepository extends Repository<HoSoBenhAn, Long> {

    /** Hồ sơ bệnh án của các lịch hẹn cho trước, kèm các dòng đơn thuốc và thuốc. */
    @Query("""
            select distinct b from HoSoBenhAn b
            left join fetch b.chiTietDonThuoc c
            left join fetch c.thuoc
            where b.lichHen.id in :lichHenIds
            """)
    List<HoSoBenhAn> timTheoLichHenKemDonThuoc(Collection<Long> lichHenIds);

}
