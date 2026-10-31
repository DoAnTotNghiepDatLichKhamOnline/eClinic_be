package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;

/**
 * Các lần quản trị viên từ chối gắn hồ sơ vào tài khoản: mảng JSON ho_so_benh_nhan.tu_choi_lien_ket. Chỉ đọc / ghi bằng
 * các câu SQL riêng này, không qua entity (bản HoSoBenhNhan cũ trong bộ nhớ không ghi đè được mảng).
 */
public interface LienKetHoSoBiTuChoiRepository extends Repository<HoSoBenhNhan, Long> {

    /** 1 nếu quản trị viên đã từ chối gắn hồ sơ này vào tài khoản này, 0 nếu chưa; có thì không đưa lại vào hàng chờ. */
    @Query(value = """
            select count(*) from ho_so_benh_nhan
            where id_ho_so_benh_nhan = :idHoSoBenhNhan
              and json_contains(tu_choi_lien_ket, json_object('idTaiKhoan', :idTaiKhoan))
            """, nativeQuery = true)
    long demTuChoi(Long idTaiKhoan, Long idHoSoBenhNhan);

    /** Thêm 1 lần từ chối vào mảng; không xoá persistence context vì hồ sơ đang khoá còn thay đổi chưa flush. */
    @Modifying
    @Query(value = """
            update ho_so_benh_nhan
            set tu_choi_lien_ket = json_array_append(coalesce(tu_choi_lien_ket, json_array()), '$',
                    json_object('idTaiKhoan', :idTaiKhoan, 'lyDo', :lyDo, 'ngayTao', :ngayTao))
            where id_ho_so_benh_nhan = :idHoSoBenhNhan
            """, nativeQuery = true)
    int ghiTuChoi(Long idHoSoBenhNhan, Long idTaiKhoan, String lyDo, LocalDateTime ngayTao);

}
