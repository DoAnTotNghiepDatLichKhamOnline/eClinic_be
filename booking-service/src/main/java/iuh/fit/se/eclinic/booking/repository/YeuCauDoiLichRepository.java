package iuh.fit.se.eclinic.booking.repository;

import java.util.Collection;
import java.util.List;
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
            left join fetch y.bacSiNhan bn
            left join fetch bn.taiKhoan
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

    /** Gọi với {@link TrangThaiYeuCau#DANG_CHO}: ca đã có yêu cầu còn chờ (đồng nghiệp hoặc quản trị viên). */
    boolean existsByLichLamViecIdAndTrangThaiIn(Long lichLamViecId, Collection<TrangThaiYeuCau> trangThai);

    /** Gọi với {@link TrangThaiYeuCau#DANG_CHO}: yêu cầu còn chờ của ca (tối đa 1, UNIQUE trong DB). */
    Optional<YeuCauDoiLich> findByLichLamViecIdAndTrangThaiIn(Long lichLamViecId, Collection<TrangThaiYeuCau> trangThai);

    /** Yêu cầu nhường ca gửi cho 1 bác sĩ (bác sĩ nhận), mới gửi nhất trước; {@code trangThai} null = mọi trạng thái. */
    @Query(value = KEM_CHI_TIET + """
            where bn.id = :idBacSiNhan and (:trangThai is null or y.trangThai = :trangThai)
            order by y.ngayGui desc, y.id desc
            """, countQuery = """
            select count(y) from YeuCauDoiLich y
            where y.bacSiNhan.id = :idBacSiNhan and (:trangThai is null or y.trangThai = :trangThai)
            """)
    Page<YeuCauDoiLich> timGuiChoBacSi(Long idBacSiNhan, TrangThaiYeuCau trangThai, Pageable pageable);

    /** 1 yêu cầu của nhóm: id, id ca, id bác sĩ gửi. Đọc không khoá, để nơi gọi khoá ca rồi yêu cầu theo thứ tự id. */
    interface ThanhVienNhom {
        Long getIdYeuCau();

        Long getIdLichLamViec();

        Long getIdBacSi();
    }

    /** Mọi yêu cầu của 1 nhóm xin nghỉ nhiều ca, theo id ca tăng dần (thứ tự khoá). */
    @Query("""
            select y.id as idYeuCau, y.lichLamViec.id as idLichLamViec, y.bacSi.id as idBacSi
            from YeuCauDoiLich y where y.maNhom = :maNhom order by y.lichLamViec.id
            """)
    List<ThanhVienNhom> timTheoNhom(String maNhom);

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
