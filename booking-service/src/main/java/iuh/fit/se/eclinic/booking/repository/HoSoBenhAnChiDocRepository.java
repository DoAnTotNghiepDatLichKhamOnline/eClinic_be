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
     * Sổ khám bệnh theo mã (EXAM-03): hồ sơ bệnh án kèm lịch hẹn và mọi thứ trang sổ khám hiển thị (hồ sơ bệnh nhân,
     * người giám hộ, bác sĩ + tài khoản + chuyên khoa, phòng khám, lượt khám + ca). Đơn thuốc tải sau bằng
     * {@link #timTheoLichHenKemDonThuoc}. Cột mã dùng collation không phân biệt hoa thường nên nơi gọi phải so lại chính
     * xác bằng {@code equals}.
     */
    @Query("""
            select b from HoSoBenhAn b
            join fetch b.lichHen l
            join fetch l.hoSoBenhNhan
            left join fetch l.nguoiGiamHo
            join fetch l.bacSi bs
            join fetch bs.taiKhoan
            join fetch bs.chuyenKhoa
            join fetch l.phongKham
            join fetch l.khungGio k
            join fetch k.lichLamViec
            where b.maTokenSoKham = :maSoKham
            """)
    Optional<HoSoBenhAn> timTheoMaSoKham(String maSoKham);

    /** Mã sổ khám của lượt khám của 1 lịch hẹn (lịch hẹn chưa khám thì không có), để cấp lại link sổ khám. */
    @Query("select b.maTokenSoKham from HoSoBenhAn b where b.lichHen.id = :idLichHen")
    Optional<String> timMaSoKhamTheoLichHen(Long idLichHen);

    /** Chỉ mã sổ khám (cho ảnh QR); collation như trên nên nơi gọi so lại bằng {@code equals}. */
    @Query("select b.maTokenSoKham from HoSoBenhAn b where b.maTokenSoKham = :maSoKham")
    Optional<String> timMaSoKham(String maSoKham);

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
