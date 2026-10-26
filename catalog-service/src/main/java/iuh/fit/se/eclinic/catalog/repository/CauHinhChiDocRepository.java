package iuh.fit.se.eclinic.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;

/**
 * Chỉ đọc: quy tắc đặt lịch thuộc booking-service (bảng 1 dòng {@code cau_hinh_he_thong}). Dùng cho bộ lọc "còn chỗ" của
 * danh sách bác sĩ công khai. Đọc thẳng từ DB mỗi lần: booking-service giữ bản đệm của riêng nó, quản trị viên sửa
 * thì có hiệu lực ngay ở đây.
 */
public interface CauHinhChiDocRepository extends Repository<CauHinhHeThong, Integer> {

    interface QuyTacDatLich {

        int getDatTruocToiThieuPhut();

        int getSoNgayDatTruocToiDa();
    }

    /** Rỗng khi booking-service chưa ghi dòng cấu hình đầu tiên. */
    @Query("""
            select c.datTruocToiThieuPhut as datTruocToiThieuPhut, c.soNgayDatTruocToiDa as soNgayDatTruocToiDa
            from CauHinhHeThong c
            where c.id = :id
            """)
    Optional<QuyTacDatLich> timQuyTacDatLich(Integer id);

}
