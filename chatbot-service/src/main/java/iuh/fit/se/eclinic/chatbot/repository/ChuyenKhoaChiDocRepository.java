package iuh.fit.se.eclinic.chatbot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;

/**
 * CHỈ ĐỌC chuyên khoa (bảng của catalog-service) kèm triệu chứng thường gặp, để chatbot chọn chuyên khoa theo lời người
 * dùng kể. Kế thừa {@link Repository} chứ không phải JpaRepository nên không có save / delete.
 */
public interface ChuyenKhoaChiDocRepository extends Repository<ChuyenKhoa, Long> {

    List<ChuyenKhoa> findAllByOrderByIdAsc();

    Optional<ChuyenKhoa> findById(Long id);

}
