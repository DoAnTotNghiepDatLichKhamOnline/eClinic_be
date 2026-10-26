package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.scheduling.NgayNghiBenhVien;

/** Ngày bệnh viện nghỉ: chỉ thêm và xoá, không sửa (xem {@link NgayNghiBenhVien}). */
public interface NgayNghiBenhVienRepository extends Repository<NgayNghiBenhVien, Long> {

    Optional<NgayNghiBenhVien> findByNgay(LocalDate ngay);

    /** Ngày nghỉ trong [tuNgay, denNgay], theo ngày; lấy sẵn quản trị viên đã khai báo và tài khoản. */
    @Query("""
            select n from NgayNghiBenhVien n
              join fetch n.quanTriTao q
              join fetch q.taiKhoan
            where n.ngay between :tuNgay and :denNgay
            order by n.ngay
            """)
    List<NgayNghiBenhVien> timTrongKhoang(LocalDate tuNgay, LocalDate denNgay);

    <S extends NgayNghiBenhVien> List<S> saveAll(Iterable<S> cacNgay);

    void delete(NgayNghiBenhVien ngayNghi);

}
