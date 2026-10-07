package iuh.fit.se.eclinic.medical.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import jakarta.persistence.LockModeType;

public interface ThuocRepository extends JpaRepository<Thuoc, Long> {

    Optional<Thuoc> findByTenChuanHoa(String tenChuanHoa);

    /**
     * Đọc có khoá (SELECT ... FOR SHARE): luôn thấy bản ghi mới nhất đã commit,
     * kể cả khi transaction hiện tại đã có snapshot cũ (REPEATABLE READ).
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select t from Thuoc t where t.tenChuanHoa = :tenChuanHoa")
    Optional<Thuoc> findByTenChuanHoaForShare(String tenChuanHoa);

    /** Gợi ý tên thuốc khi bác sĩ gõ; chỉ thuốc ở trạng thái đã cho (thuốc ngừng dùng không được gợi ý). */
    List<Thuoc> findTop20ByTenChuanHoaContainingAndTrangThaiOrderByTenThuocAsc(String keyword,
            TrangThaiThuoc trangThai);

    boolean existsByTenChuanHoa(String tenChuanHoa);

    boolean existsByTenChuanHoaAndIdNot(String tenChuanHoa, Long id);

    /** Khoá dòng thuốc khi quản trị viên sửa / đổi trạng thái, để 2 thao tác cùng lúc chạy lần lượt. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Thuoc t where t.id = :id")
    Optional<Thuoc> findByIdForUpdate(Long id);

    /**
     * Danh mục thuốc của quản trị viên: thuốc chưa xác minh trước, rồi theo tên; kèm bác sĩ đã thêm (nếu có).
     *
     * @param mau       mẫu LIKE (escape '!') so với tên đã chuẩn hoá; null = không lọc
     * @param daXacMinh null = cả hai
     * @param trangThai null = mọi trạng thái
     */
    @Query(value = """
            select t from Thuoc t
              left join fetch t.bacSiTao b
              left join fetch b.taiKhoan
            where (:mau is null or t.tenChuanHoa like :mau escape '!')
              and (:daXacMinh is null or t.daXacMinh = :daXacMinh)
              and (:trangThai is null or t.trangThai = :trangThai)
            order by t.daXacMinh asc, t.tenThuoc asc, t.id asc
            """, countQuery = """
            select count(t) from Thuoc t
            where (:mau is null or t.tenChuanHoa like :mau escape '!')
              and (:daXacMinh is null or t.daXacMinh = :daXacMinh)
              and (:trangThai is null or t.trangThai = :trangThai)
            """)
    Page<Thuoc> timChoQuanTri(String mau, Boolean daXacMinh, TrangThaiThuoc trangThai, Pageable pageable);

    interface SoLanKeTheoThuoc {

        Long getIdThuoc();

        long getSoLanKe();
    }

    /** Số dòng đơn thuốc dùng từng thuốc trong danh sách; thuốc chưa được kê lần nào không có dòng. */
    @Query("""
            select c.thuoc.id as idThuoc, count(c) as soLanKe from ChiTietDonThuoc c
            where c.thuoc.id in :idCacThuoc
            group by c.thuoc.id
            """)
    List<SoLanKeTheoThuoc> demSoLanKe(Collection<Long> idCacThuoc);

    @Query("select count(c) from ChiTietDonThuoc c where c.thuoc.id = :idThuoc")
    long demSoLanKe(Long idThuoc);

    /**
     * Thêm thuốc nếu chưa có; trùng ten_chuan_hoa thì bỏ qua (không ném lỗi, không làm hỏng transaction).
     */
    @Modifying
    @Query(value = """
            INSERT INTO thuoc (ten_thuoc, ten_chuan_hoa, don_vi, da_xac_minh, id_bac_si_tao)
            VALUES (:tenThuoc, :tenChuanHoa, :donVi, FALSE, :idBacSiTao)
            ON DUPLICATE KEY UPDATE id_thuoc = id_thuoc
            """, nativeQuery = true)
    int insertIfAbsent(String tenThuoc, String tenChuanHoa, String donVi, Long idBacSiTao);

}
