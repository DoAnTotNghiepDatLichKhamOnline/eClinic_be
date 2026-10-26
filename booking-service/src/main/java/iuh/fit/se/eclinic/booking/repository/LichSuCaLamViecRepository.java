package iuh.fit.se.eclinic.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.scheduling.LichSuCaLamViec;

/**
 * Lịch sử thay đổi ca làm việc. Chỉ thêm và đọc: kế thừa {@link Repository} nên không có update / delete.
 */
public interface LichSuCaLamViecRepository extends Repository<LichSuCaLamViec, Long> {

    LichSuCaLamViec save(LichSuCaLamViec lichSu);

    /** Mọi dòng lịch sử của 1 ca, cũ nhất trước; lấy sẵn quản trị viên, bác sĩ, phòng khám. */
    @Query("""
            select l from LichSuCaLamViec l
              join fetch l.quanTri q
              join fetch q.taiKhoan
              join fetch l.bacSi b
              join fetch b.taiKhoan
              join fetch l.phongKham
            where l.lichLamViec.id = :idLichLamViec
            order by l.ngayTao, l.id
            """)
    List<LichSuCaLamViec> timTheoCa(Long idLichLamViec);

}
