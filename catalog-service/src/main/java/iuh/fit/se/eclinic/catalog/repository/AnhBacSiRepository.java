package iuh.fit.se.eclinic.catalog.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.AnhBacSi;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Ảnh giới thiệu của bác sĩ: mảng JSON ở bac_si.anh_gioi_thieu (V45). Entity BacSi không ánh xạ cột này (lần lưu hồ sơ
 * không ghi đè ảnh), nên đọc / ghi bằng SQL riêng. Mọi lần sửa: {@link #khoa} rồi {@link #ghi} trong cùng transaction.
 */
@Repository
@RequiredArgsConstructor
public class AnhBacSiRepository {

    private final EntityManager entityManager;

    /** Ảnh của bác sĩ theo thứ tự hiển thị; bác sĩ không có thì rỗng. */
    public List<AnhBacSi> findByBacSiIdOrderByThuTuAscIdAsc(Long bacSiId) {
        return doc(bacSiId, "");
    }

    public long countByBacSiId(Long bacSiId) {
        return findByBacSiIdOrderByThuTuAscIdAsc(bacSiId).size();
    }

    /** Khoá dòng bác sĩ tới hết transaction (lần sửa ảnh đồng thời phải chờ) rồi trả danh sách ảnh hiện tại. */
    public List<AnhBacSi> khoa(Long bacSiId) {
        return doc(bacSiId, " FOR UPDATE");
    }

    /** Ghi lại cả danh sách ảnh (rỗng thành NULL); gọi sau {@link #khoa} trong cùng transaction. */
    public void ghi(Long bacSiId, List<AnhBacSi> danhSach) {
        if (danhSach.isEmpty()) {
            entityManager.createNativeQuery("UPDATE bac_si SET anh_gioi_thieu = NULL WHERE id_bac_si = :id")
                    .setParameter("id", bacSiId)
                    .executeUpdate();
            return;
        }
        entityManager.createNativeQuery("UPDATE bac_si SET anh_gioi_thieu = CAST(:json AS JSON) WHERE id_bac_si = :id")
                .setParameter("json", AnhBacSi.vietDanhSach(danhSach))
                .setParameter("id", bacSiId)
                .executeUpdate();
    }

    /** id cho ảnh mới của bác sĩ: tăng bộ đếm riêng của bác sĩ, id của ảnh đã xoá không được cấp lại. */
    public long idMoi(Long bacSiId) {
        entityManager.createNativeQuery("UPDATE bac_si SET anh_id_cuoi = anh_id_cuoi + 1 WHERE id_bac_si = :id")
                .setParameter("id", bacSiId)
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT anh_id_cuoi FROM bac_si WHERE id_bac_si = :id")
                .setParameter("id", bacSiId)
                .getSingleResult()).longValue();
    }

    // CAST: Hibernate chỉ thấy chữ, không phải ánh xạ kiểu JSON
    private List<AnhBacSi> doc(Long bacSiId, String khoa) {
        List<String> ketQua = entityManager.createNativeQuery(
                        "SELECT CAST(anh_gioi_thieu AS CHAR) FROM bac_si WHERE id_bac_si = :id" + khoa, String.class)
                .setParameter("id", bacSiId)
                .getResultList();
        return AnhBacSi.docDanhSach(ketQua.isEmpty() ? null : ketQua.get(0));
    }

}
