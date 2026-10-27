package iuh.fit.se.eclinic.booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichSuCauHinh;

/** Lịch sử sửa cấu hình. */
public interface LichSuCauHinhRepository extends Repository<LichSuCauHinh, Long> {

    LichSuCauHinh save(LichSuCauHinh lichSu);

    /** Mới nhất trước; lấy sẵn quản trị viên và tài khoản. */
    @Query(value = """
            select l from LichSuCauHinh l
              join fetch l.quanTri q
              join fetch q.taiKhoan
            order by l.ngayTao desc, l.id desc
            """, countQuery = "select count(l) from LichSuCauHinh l")
    Page<LichSuCauHinh> timMoiNhatTruoc(Pageable pageable);

}
