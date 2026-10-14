package iuh.fit.se.eclinic.medical.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.medical.LichSuSuaBenhAn;

/**
 * Lịch sử sửa hồ sơ bệnh án, CHỈ THÊM: kế thừa {@link Repository} nên chỉ có những hàm khai báo ở đây (không update /
 * delete), entity lại là {@code @Immutable}.
 */
public interface LichSuSuaBenhAnRepository extends Repository<LichSuSuaBenhAn, Long> {

    LichSuSuaBenhAn save(LichSuSuaBenhAn lanSua);

    /** Các lần sửa của 1 hồ sơ bệnh án kèm tài khoản bác sĩ đã sửa, mới nhất trước. */
    @Query("""
            select s from LichSuSuaBenhAn s
            join fetch s.bacSi b
            join fetch b.taiKhoan
            where s.hoSoBenhAn.id = :idHoSoBenhAn
            order by s.phienBan desc
            """)
    List<LichSuSuaBenhAn> timCuaHoSoBenhAn(Long idHoSoBenhAn);

}
