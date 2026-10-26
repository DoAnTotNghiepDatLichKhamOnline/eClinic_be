package iuh.fit.se.eclinic.booking.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichSuLichHen;

/** Nhật ký thao tác trên lịch hẹn: chỉ thêm và đọc (xem {@link LichSuLichHen}). */
public interface LichSuLichHenRepository extends Repository<LichSuLichHen, Long> {

    LichSuLichHen save(LichSuLichHen dong);

    /** Các dòng của những lịch hẹn này, cũ nhất trước; lấy sẵn lịch hẹn, tài khoản đã làm và lịch hẹn liên quan. */
    @Query("""
            select s from LichSuLichHen s
              join fetch s.lichHen
              left join fetch s.taiKhoanThucHien
              left join fetch s.lichHenLienQuan
            where s.lichHen.id in :cacIdLichHen
            order by s.ngayTao asc, s.id asc
            """)
    List<LichSuLichHen> timTheoLichHen(Collection<Long> cacIdLichHen);

}
