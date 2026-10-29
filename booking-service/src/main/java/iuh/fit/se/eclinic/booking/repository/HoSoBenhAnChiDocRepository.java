package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

/** Chỉ đọc hồ sơ bệnh án (bảng của medical-service) cho hồ sơ khám, sổ khám và nhắc tái khám. */
public interface HoSoBenhAnChiDocRepository extends Repository<HoSoBenhAn, Long> {

    /** Hồ sơ bệnh án của các lịch hẹn cho trước, kèm các dòng đơn thuốc, thuốc. */
    @Query("""
            select distinct b from HoSoBenhAn b
            left join fetch b.chiTietDonThuoc c
            left join fetch c.thuoc
            where b.lichHen.id in :lichHenIds
            """)
    List<HoSoBenhAn> timTheoLichHenKemDonThuoc(Collection<Long> lichHenIds);

    Optional<HoSoBenhAn> findById(Long id);

    /** Sổ khám bệnh theo mã: hồ sơ bệnh án kèm mọi thứ trang sổ khám hiển thị. */
    @Query("""
            select b from HoSoBenhAn b
            join fetch b.lichHen l
            join fetch l.hoSoBenhNhan
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

    /** Id hồ sơ bệnh án có ngày tái khám sau {@code homNay} mà bệnh nhân chưa được báo đúng ngày đó. */
    @Query("""
            select b.id from HoSoBenhAn b
            where b.ngayTaiKhamDeXuat > :homNay
              and (b.taiKhamNgayDaBao is null or b.taiKhamNgayDaBao <> b.ngayTaiKhamDeXuat)
            order by b.id asc
            """)
    List<Long> timIdCanBaoTaiKham(LocalDate homNay, Limit gioiHan);

    /** Id hồ sơ bệnh án đến hạn nhắc trước ngày tái khám, ngày tái khám trong ({@code homNay}, {@code den}]. */
    @Query("""
            select b.id from HoSoBenhAn b
            where b.taiKhamNgayDaBao = b.ngayTaiKhamDeXuat and b.taiKhamDaNhacTruoc = false
              and b.ngayTaiKhamDeXuat > :homNay and b.ngayTaiKhamDeXuat <= :den
            order by b.ngayTaiKhamDeXuat asc, b.id asc
            """)
    List<Long> timIdCanNhacTaiKham(LocalDate homNay, LocalDate den, Limit gioiHan);

}
