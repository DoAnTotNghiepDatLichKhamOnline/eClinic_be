package iuh.fit.se.eclinic.report.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;

/**
 * Chỉ đọc: ca làm việc và lượt khám thuộc booking-service. Chỉ tính ca HOAT_DONG: ca bị hủy (xin nghỉ, ngày bệnh viện
 * nghỉ...) không còn lượt nào đặt được nên không thuộc sức chứa.
 */
public interface ThongKeCaRepository extends Repository<LichLamViec, Long> {

    interface SoLuotTheoBacSi {

        Long getIdBacSi();

        TrangThaiKhungGio getTrangThai();

        long getSoLuong();
    }

    /** Lượt còn trống / đã đặt của các ca hoạt động trong khoảng ngày, theo bác sĩ (lượt đã bỏ của ca không tính). */
    @Query("""
            select c.bacSi.id as idBacSi, k.trangThai as trangThai, count(k) as soLuong
            from KhungGioKham k join k.lichLamViec c
            where c.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and c.ngayLamViec between :tuNgay and :denNgay
              and k.trangThai in (iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio.CON_TRONG,
                                  iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio.DA_DAT)
              and (:idChuyenKhoa is null or c.bacSi.chuyenKhoa.id = :idChuyenKhoa)
            group by c.bacSi.id, k.trangThai
            """)
    List<SoLuotTheoBacSi> demLuotTheoBacSi(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa);

    /** Số bác sĩ có ít nhất 1 ca hoạt động trong khoảng ngày. */
    @Query("""
            select count(distinct c.bacSi.id) from LichLamViec c
            where c.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and c.ngayLamViec between :tuNgay and :denNgay
              and (:idChuyenKhoa is null or c.bacSi.chuyenKhoa.id = :idChuyenKhoa)
            """)
    long demBacSiCoCa(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa);

}
