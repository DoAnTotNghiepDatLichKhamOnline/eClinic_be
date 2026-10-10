package iuh.fit.se.eclinic.booking.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;
import jakarta.persistence.LockModeType;

public interface YeuCauDoiLichRepository extends JpaRepository<YeuCauDoiLich, Long> {

    /** Yêu cầu kèm mọi thứ danh sách hiển thị; các quan hệ đều là *-to-one nên phân trang được. */
    String KEM_CHI_TIET = """
            select y from YeuCauDoiLich y
            join fetch y.bacSi b
            join fetch b.taiKhoan
            join fetch b.chuyenKhoa
            join fetch y.lichLamViec l
            join fetch l.phongKham
            left join fetch y.phongKhamMongMuon
            """;

    /** Khoá dòng yêu cầu (SELECT ... FOR UPDATE, không join) trước khi duyệt / từ chối / rút. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select y from YeuCauDoiLich y where y.id = :id")
    Optional<YeuCauDoiLich> findByIdForUpdate(Long id);

    /**
     * Id ca của 1 yêu cầu, không tải entity: nơi gọi khoá ca trước rồi mới khoá yêu cầu, và entity đã nằm trong
     * persistence context thì câu khoá sau đó không đọc lại trạng thái mới.
     */
    @Query("select y.lichLamViec.id from YeuCauDoiLich y where y.id = :id")
    Optional<Long> timIdLichLamViec(Long id);

    boolean existsByLichLamViecIdAndTrangThai(Long lichLamViecId, TrangThaiYeuCau trangThai);

    Optional<YeuCauDoiLich> findByLichLamViecIdAndTrangThai(Long lichLamViecId, TrangThaiYeuCau trangThai);

    /** Yêu cầu của 1 bác sĩ, mới gửi nhất trước; {@code trangThai} null = mọi trạng thái. */
    @Query(value = KEM_CHI_TIET + """
            where b.id = :idBacSi and (:trangThai is null or y.trangThai = :trangThai)
            order by y.ngayGui desc, y.id desc
            """, countQuery = """
            select count(y) from YeuCauDoiLich y
            where y.bacSi.id = :idBacSi and (:trangThai is null or y.trangThai = :trangThai)
            """)
    Page<YeuCauDoiLich> timCuaBacSi(Long idBacSi, TrangThaiYeuCau trangThai, Pageable pageable);

    /** Yêu cầu của mọi bác sĩ cho quản trị viên, gửi sớm nhất trước (xử lý theo thứ tự gửi); null = mọi trạng thái. */
    @Query(value = KEM_CHI_TIET + """
            where (:trangThai is null or y.trangThai = :trangThai)
            order by y.ngayGui asc, y.id asc
            """, countQuery = """
            select count(y) from YeuCauDoiLich y where (:trangThai is null or y.trangThai = :trangThai)
            """)
    Page<YeuCauDoiLich> timTatCa(TrangThaiYeuCau trangThai, Pageable pageable);

}
