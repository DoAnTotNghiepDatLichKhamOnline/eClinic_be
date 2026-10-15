package iuh.fit.se.eclinic.medical.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.medical.Icd10;

public interface Icd10Repository extends JpaRepository<Icd10, String> {

    /**
     * Gợi ý khi bác sĩ gõ: mã bắt đầu bằng từ khoá, hoặc tên tiếng Việt / tiếng Anh chứa từ khoá (collation của DB không
     * phân biệt hoa thường, dấu), theo mã.
     *
     * @param tienTo mẫu LIKE "bắt đầu bằng" (escape '!')
     * @param mau    mẫu LIKE "chứa" (escape '!')
     */
    @Query("""
            select i from Icd10 i
            where i.ma like :tienTo escape '!' or i.ten like :mau escape '!' or i.tenEn like :mau escape '!'
            order by i.ma asc
            """)
    List<Icd10> goiY(String tienTo, String mau, Pageable pageable);

}
