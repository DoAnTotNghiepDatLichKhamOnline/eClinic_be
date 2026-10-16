package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

/**
 * CHỈ ĐỌC hồ sơ bệnh án (bảng của medical-service): để bác sĩ xem kết quả các lần khám trước trong hồ sơ khám của bệnh
 * nhân, và để job nhắc tái khám biết ngày tái khám bác sĩ đề xuất. Kế thừa {@link Repository} chứ không phải JpaRepository nên không có save / delete.
 */
public interface HoSoBenhAnChiDocRepository extends Repository<HoSoBenhAn, Long> {

    /** Hồ sơ bệnh án của các lịch hẹn cho trước, kèm các dòng đơn thuốc, thuốc và mã ICD-10. */
    @Query("""
            select distinct b from HoSoBenhAn b
            left join fetch b.icdChinh
            left join fetch b.chiTietDonThuoc c
            left join fetch c.thuoc
            where b.lichHen.id in :lichHenIds
            """)
    List<HoSoBenhAn> timTheoLichHenKemDonThuoc(Collection<Long> lichHenIds);

    Optional<HoSoBenhAn> findById(Long id);

    /**
     * Id các hồ sơ bệnh án có ngày tái khám ở sau {@code homNay} mà bệnh nhân chưa được báo đúng ngày đó (chưa có dòng
     * nhac_tai_kham, hoặc ngày đã báo là ngày khác / đã bị xoá).
     */
    @Query("""
            select b.id from HoSoBenhAn b
            where b.ngayTaiKhamDeXuat > :homNay
              and not exists (select 1 from NhacTaiKham n
                              where n.idHoSoBenhAn = b.id and n.ngayDaBao = b.ngayTaiKhamDeXuat)
            order by b.id asc
            """)
    List<Long> timIdCanBaoTaiKham(LocalDate homNay, Limit gioiHan);

    /**
     * Id các hồ sơ bệnh án đến hạn nhắc trước ngày tái khám: ngày đã báo vẫn là ngày hiện tại, chưa xử lý lần nhắc
     * trước, và ngày tái khám nằm trong ({@code homNay}, {@code den}]. Ngày gần nhất trước.
     */
    @Query("""
            select b.id from HoSoBenhAn b, NhacTaiKham n
            where n.idHoSoBenhAn = b.id and n.ngayDaBao = b.ngayTaiKhamDeXuat and n.daNhacTruoc = false
              and b.ngayTaiKhamDeXuat > :homNay and b.ngayTaiKhamDeXuat <= :den
            order by b.ngayTaiKhamDeXuat asc, b.id asc
            """)
    List<Long> timIdCanNhacTaiKham(LocalDate homNay, LocalDate den, Limit gioiHan);

}
