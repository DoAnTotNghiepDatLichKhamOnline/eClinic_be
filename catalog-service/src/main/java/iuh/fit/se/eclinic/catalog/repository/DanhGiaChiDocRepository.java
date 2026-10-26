package iuh.fit.se.eclinic.catalog.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.DanhGia;

/** Chỉ đọc: đánh giá thuộc booking-service. Dùng cho điểm đánh giá trên danh sách / chi tiết bác sĩ công khai. */
public interface DanhGiaChiDocRepository extends Repository<DanhGia, Long> {

    interface DiemTheoBacSi {

        Long getIdBacSi();

        Double getDiemTrungBinh();

        long getSoDanhGia();
    }

    /** Điểm trung bình (chưa làm tròn) và số đánh giá của từng bác sĩ; bác sĩ chưa có đánh giá nào không có dòng. */
    @Query("""
            select d.bacSi.id as idBacSi, avg(d.soSao) as diemTrungBinh, count(d) as soDanhGia from DanhGia d
            where d.bacSi.id in :idCacBacSi
            group by d.bacSi.id
            """)
    List<DiemTheoBacSi> tinhDiem(Collection<Long> idCacBacSi);
}
