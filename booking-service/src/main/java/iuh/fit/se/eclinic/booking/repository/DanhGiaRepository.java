package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.DanhGia;

/**
 * Đánh giá là các cột danh_gia_* của lịch hẹn ({@link DanhGia} chỉ đọc). Gửi / sửa bằng câu UPDATE chỉ đụng các cột đó:
 * lịch hẹn không bị tăng version, không đổi ngay_cap_nhat.
 */
public interface DanhGiaRepository extends Repository<DanhGia, Long> {

    Optional<DanhGia> findByLichHenId(Long lichHenId);

    /** Ghi đánh giá cho lịch hẹn chưa có đánh giá; trả 0 nếu lịch hẹn đã được đánh giá. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update lich_hen
            set danh_gia_so_sao = :soSao, danh_gia_nhan_xet = :nhanXet, danh_gia_id_tai_khoan = :idTaiKhoan,
                danh_gia_ngay_tao = :bayGio, danh_gia_ngay_cap_nhat = :bayGio
            where id_lich_hen = :idLichHen and danh_gia_so_sao is null
            """, nativeQuery = true)
    int gui(Long idLichHen, int soSao, String nhanXet, Long idTaiKhoan, LocalDateTime bayGio);

    /** Thay số sao và nhận xét của đánh giá đã có. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update lich_hen
            set danh_gia_so_sao = :soSao, danh_gia_nhan_xet = :nhanXet, danh_gia_ngay_cap_nhat = :bayGio
            where id_lich_hen = :idLichHen and danh_gia_so_sao is not null
            """, nativeQuery = true)
    int sua(Long idLichHen, int soSao, String nhanXet, LocalDateTime bayGio);

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

    /** Đánh giá cho quản trị viên, mới nhất trước, kèm lịch hẹn, hồ sơ bệnh nhân, bác sĩ. */
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
