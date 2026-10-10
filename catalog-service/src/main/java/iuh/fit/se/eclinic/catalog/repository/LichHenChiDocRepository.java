package iuh.fit.se.eclinic.catalog.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

/** Chỉ đọc: lịch hẹn thuộc booking-service. Dùng cho danh bạ bác sĩ của quản trị viên. */
public interface LichHenChiDocRepository extends Repository<LichHen, Long> {

    interface SoLuotTheoBacSi {

        Long getIdBacSi();

        long getSoLuot();
    }

    /** Số lịch hẹn đã khám xong của từng bác sĩ trong danh sách; bác sĩ chưa khám lượt nào không có dòng. */
    @Query("""
            select l.bacSi.id as idBacSi, count(l) as soLuot from LichHen l
            where l.bacSi.id in :idCacBacSi
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichHen.DA_HOAN_THANH
            group by l.bacSi.id
            """)
    List<SoLuotTheoBacSi> demDaKham(Collection<Long> idCacBacSi);

    /**
     * Số lịch hẹn còn hiệu lực (chờ xác nhận, đã xác nhận; chưa bị đánh dấu cần đổi lịch) nằm trên các ca còn hoạt động
     * chưa bắt đầu của bác sĩ: những lịch hẹn sẽ phải đổi lịch nếu bác sĩ ngừng công tác.
     */
    @Query("""
            select count(l) from LichHen l
              join l.khungGio k
              join k.lichLamViec c
            where c.bacSi.id = :idBacSi
              and c.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (c.ngayLamViec > :homNay or (c.ngayLamViec = :homNay and c.gioBatDau > :gioHienTai))
              and l.trangThai in (iuh.fit.se.eclinic.common.enums.TrangThaiLichHen.CHO_XAC_NHAN,
                                  iuh.fit.se.eclinic.common.enums.TrangThaiLichHen.DA_XAC_NHAN)
              and l.canDoiLich = false
            """)
    long demBiAnhHuongNeuNgungCongTac(Long idBacSi, LocalDate homNay, LocalTime gioHienTai);

}
