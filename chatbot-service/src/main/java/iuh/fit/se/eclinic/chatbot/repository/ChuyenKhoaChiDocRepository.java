package iuh.fit.se.eclinic.chatbot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;

/** Chỉ đọc chuyên khoa (bảng của catalog-service) kèm triệu chứng, để chatbot chọn chuyên khoa. */
public interface ChuyenKhoaChiDocRepository extends Repository<ChuyenKhoa, Long> {

    List<ChuyenKhoa> findAllByOrderByIdAsc();

    Optional<ChuyenKhoa> findById(Long id);

}
