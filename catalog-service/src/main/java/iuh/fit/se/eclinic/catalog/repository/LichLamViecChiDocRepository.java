package iuh.fit.se.eclinic.catalog.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

/** Chỉ đọc: ca làm việc thuộc booking-service. */
public interface LichLamViecChiDocRepository extends Repository<LichLamViec, Long> {

    /** Số ca còn hoạt động chưa bắt đầu của bác sĩ (ca đang diễn ra không tính). */
    @Query("""
            select count(c) from LichLamViec c
            where c.bacSi.id = :idBacSi
              and c.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (c.ngayLamViec > :homNay or (c.ngayLamViec = :homNay and c.gioBatDau > :gioHienTai))
            """)
    long demCaSapToi(Long idBacSi, LocalDate homNay, LocalTime gioHienTai);

    /** Số ca còn hoạt động chưa bắt đầu đang xếp ở phòng khám (ca đang diễn ra không tính). */
    @Query("""
            select count(c) from LichLamViec c
            where c.phongKham.id = :idPhongKham
              and c.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (c.ngayLamViec > :homNay or (c.ngayLamViec = :homNay and c.gioBatDau > :gioHienTai))
            """)
    long demCaSapToiCuaPhong(Long idPhongKham, LocalDate homNay, LocalTime gioHienTai);

    interface SoCaTheoPhong {

        Long getIdPhongKham();

        long getSoCa();
    }

    /** Như {@link #demCaSapToiCuaPhong} cho nhiều phòng; phòng không có ca sắp tới không có dòng. */
    @Query("""
            select c.phongKham.id as idPhongKham, count(c) as soCa from LichLamViec c
            where c.phongKham.id in :idCacPhong
              and c.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec.HOAT_DONG
              and (c.ngayLamViec > :homNay or (c.ngayLamViec = :homNay and c.gioBatDau > :gioHienTai))
            group by c.phongKham.id
            """)
    List<SoCaTheoPhong> demCaSapToiTheoPhong(Collection<Long> idCacPhong, LocalDate homNay, LocalTime gioHienTai);

}
