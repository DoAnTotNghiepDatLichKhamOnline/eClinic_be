package iuh.fit.se.eclinic.medical.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import jakarta.persistence.LockModeType;

public interface ThuocRepository extends JpaRepository<Thuoc, Long> {

    /**
     * Gợi ý khi bác sĩ gõ: chỉ thuốc kê được (đang dùng, đã xác minh), khớp tên đã chuẩn hoá hoặc hoạt chất (collation
     * của DB không phân biệt hoa thường, dấu), theo tên rồi hàm lượng.
     *
     * @param mau mẫu LIKE (escape '!')
     */
    @Query("""
            select t from Thuoc t
            where t.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiThuoc.DANG_DUNG
              and t.daXacMinh = true
              and (t.tenChuanHoa like :mau escape '!' or t.hoatChat like :mau escape '!')
            order by t.tenThuoc asc, t.hamLuong asc, t.id asc
            """)
    List<Thuoc> goiYKeDon(String mau, Pageable pageable);

    boolean existsByTenChuanHoaAndHamLuongChuanHoa(String tenChuanHoa, String hamLuongChuanHoa);

    boolean existsByTenChuanHoaAndHamLuongChuanHoaAndIdNot(String tenChuanHoa, String hamLuongChuanHoa, Long id);

    /** Khoá dòng thuốc khi quản trị viên sửa / đổi trạng thái, để 2 thao tác cùng lúc chạy lần lượt. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Thuoc t where t.id = :id")
    Optional<Thuoc> findByIdForUpdate(Long id);

    /**
     * Danh mục thuốc của quản trị viên: thuốc chưa xác minh trước, rồi theo tên, hàm lượng; kèm bác sĩ đã thêm (nếu có).
     *
     * @param mau       mẫu LIKE (escape '!') so với tên đã chuẩn hoá hoặc hoạt chất; null = không lọc
     * @param daXacMinh null = cả hai
     * @param trangThai null = mọi trạng thái
     */
    @Query(value = """
            select t from Thuoc t
              left join fetch t.bacSiTao b
              left join fetch b.taiKhoan
            where (:mau is null or t.tenChuanHoa like :mau escape '!' or t.hoatChat like :mau escape '!')
              and (:daXacMinh is null or t.daXacMinh = :daXacMinh)
              and (:trangThai is null or t.trangThai = :trangThai)
            order by t.daXacMinh asc, t.tenThuoc asc, t.hamLuong asc, t.id asc
            """, countQuery = """
            select count(t) from Thuoc t
            where (:mau is null or t.tenChuanHoa like :mau escape '!' or t.hoatChat like :mau escape '!')
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

}
