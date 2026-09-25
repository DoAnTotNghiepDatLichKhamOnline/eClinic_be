package iuh.fit.fe.be_websatlichkham.medical.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.fe.be_websatlichkham.common.entity.medical.Thuoc;
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

    /** Gợi ý tên thuốc khi bác sĩ gõ. */
    List<Thuoc> findTop20ByTenChuanHoaContainingOrderByTenThuocAsc(String keyword);

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
