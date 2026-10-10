package iuh.fit.se.eclinic.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.booking.DanhGia;

public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {

    Optional<DanhGia> findByLichHenId(Long lichHenId);

    boolean existsByLichHenId(Long lichHenId);

    interface SoDanhGiaTheoSao {

        int getSoSao();

        long getSoLuong();
    }

    /** Số đánh giá của bác sĩ theo từng mức sao; mức không có đánh giá nào không có dòng. */
    @Query("select d.soSao as soSao, count(d) as soLuong from DanhGia d where d.bacSi.id = :idBacSi group by d.soSao")
    List<SoDanhGiaTheoSao> demTheoSao(Long idBacSi);

    /** Đánh giá bác sĩ nhận được, mới nhất trước, kèm lượt khám để lấy ngày khám. */
    @Query(value = """
            select d from DanhGia d
              join fetch d.lichHen l
              join fetch l.khungGio
            where d.bacSi.id = :idBacSi
            order by d.ngayTao desc, d.id desc
            """, countQuery = "select count(d) from DanhGia d where d.bacSi.id = :idBacSi")
    Page<DanhGia> timCuaBacSi(Long idBacSi, Pageable pageable);

    /**
     * Đánh giá cho quản trị viên, mới nhất trước, kèm lịch hẹn, hồ sơ bệnh nhân, bác sĩ.
     *
     * @param idBacSi    null = mọi bác sĩ
     * @param soSaoToiDa null = mọi mức sao; có giá trị thì chỉ lấy đánh giá từ mức này trở xuống
     */
    @Query(value = """
            select d from DanhGia d
              join fetch d.lichHen l
              join fetch l.khungGio
              join fetch l.hoSoBenhNhan
              join fetch d.bacSi b
              join fetch b.taiKhoan
            where (:idBacSi is null or b.id = :idBacSi)
              and (:soSaoToiDa is null or d.soSao <= :soSaoToiDa)
            order by d.ngayTao desc, d.id desc
            """, countQuery = """
            select count(d) from DanhGia d
            where (:idBacSi is null or d.bacSi.id = :idBacSi)
              and (:soSaoToiDa is null or d.soSao <= :soSaoToiDa)
            """)
    Page<DanhGia> timChoQuanTri(Long idBacSi, Integer soSaoToiDa, Pageable pageable);
}
