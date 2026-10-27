package iuh.fit.se.eclinic.report.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** Chỉ đọc lịch hẹn của booking-service; lịch trong kỳ không tính DA_HUY_DO_DOI_LICH. */
public interface ThongKeLichHenRepository extends Repository<LichHen, Long> {

    String TRONG_KY = """
            l.khungGio.gioBatDau >= :tu and l.khungGio.gioBatDau < :den
              and l.trangThai <> iuh.fit.se.eclinic.common.enums.TrangThaiLichHen.DA_HUY_DO_DOI_LICH
              and (:idChuyenKhoa is null or l.bacSi.chuyenKhoa.id = :idChuyenKhoa)
            """;
    String DA_KHAM = " and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichHen.DA_HOAN_THANH";

    interface SoLuongTheoBacSi {

        Long getIdBacSi();

        TrangThaiLichHen getTrangThai();

        long getSoLuong();
    }

    interface SoLuongTheoNhom {

        Long getId();

        long getSoLuong();
    }

    interface SoLuongTheoKenh {

        KenhDatLich getKenhDat();

        long getSoLuong();
    }

    interface SoLuongTheoNgay {

        LocalDate getNgay();

        TrangThaiLichHen getTrangThai();

        long getSoLuong();
    }

    /** Thời gian đặt trước = giờ bắt đầu lượt khám - lúc tạo lịch hẹn (lịch đổi: lúc đổi), tính bằng phút. */
    interface ThoiGianDatTruoc {

        Double getTrungBinhPhut();

        Long getDuoi1Ngay();

        Long getTu1Den3Ngay();

        Long getTu4Den7Ngay();

        Long getTu8Den14Ngay();

        Long getTren14Ngay();
    }

    @Query("select l.bacSi.id as idBacSi, l.trangThai as trangThai, count(l) as soLuong from LichHen l where " + TRONG_KY
            + " group by l.bacSi.id, l.trangThai")
    List<SoLuongTheoBacSi> demTheoBacSiVaTrangThai(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

    /** Lịch còn chờ đổi sang ca khác vì ca của nó bị hủy (hủy ca giữ lịch hẹn, ngày bệnh viện nghỉ). */
    @Query("select count(l) from LichHen l where " + TRONG_KY + """
              and l.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiLichHen.DA_XAC_NHAN
              and l.canDoiLich = true
            """)
    long demCanDoiLich(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

    @Query("select l.kenhDat as kenhDat, count(l) as soLuong from LichHen l where " + TRONG_KY + " group by l.kenhDat")
    List<SoLuongTheoKenh> demTheoKenh(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

    /** Số bệnh nhân duy nhất đã khám: hồ sơ bệnh nhân là duy nhất theo CCCD (trẻ chưa có CCCD: theo khoá nhận diện). */
    @Query("select count(distinct l.hoSoBenhNhan.id) from LichHen l where " + TRONG_KY + DA_KHAM)
    long demBenhNhanDaKham(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

    @Query("select l.bacSi.id as id, count(distinct l.hoSoBenhNhan.id) as soLuong from LichHen l where " + TRONG_KY
            + DA_KHAM + " group by l.bacSi.id")
    List<SoLuongTheoNhom> demBenhNhanDaKhamTheoBacSi(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

    @Query("select l.bacSi.chuyenKhoa.id as id, count(distinct l.hoSoBenhNhan.id) as soLuong from LichHen l where "
            + TRONG_KY + DA_KHAM + " group by l.bacSi.chuyenKhoa.id")
    List<SoLuongTheoNhom> demBenhNhanDaKhamTheoChuyenKhoa(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

    @Query("select extract(date from l.khungGio.gioBatDau) as ngay, l.trangThai as trangThai, count(l) as soLuong"
            + " from LichHen l where " + TRONG_KY + " group by extract(date from l.khungGio.gioBatDau), l.trangThai")
    List<SoLuongTheoNgay> demTheoNgayVaTrangThai(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

    @Query("""
            select avg((l.khungGio.gioBatDau - l.ngayTao) by minute) as trungBinhPhut,
              sum(case when ((l.khungGio.gioBatDau - l.ngayTao) by minute) < 1440 then 1 else 0 end) as duoi1Ngay,
              sum(case when ((l.khungGio.gioBatDau - l.ngayTao) by minute) >= 1440
                        and ((l.khungGio.gioBatDau - l.ngayTao) by minute) < 5760 then 1 else 0 end) as tu1Den3Ngay,
              sum(case when ((l.khungGio.gioBatDau - l.ngayTao) by minute) >= 5760
                        and ((l.khungGio.gioBatDau - l.ngayTao) by minute) < 11520 then 1 else 0 end) as tu4Den7Ngay,
              sum(case when ((l.khungGio.gioBatDau - l.ngayTao) by minute) >= 11520
                        and ((l.khungGio.gioBatDau - l.ngayTao) by minute) < 21600 then 1 else 0 end) as tu8Den14Ngay,
              sum(case when ((l.khungGio.gioBatDau - l.ngayTao) by minute) >= 21600 then 1 else 0 end) as tren14Ngay
            from LichHen l
            """ + " where " + TRONG_KY)
    ThoiGianDatTruoc thoiGianDatTruoc(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

    /** Lượt đặt mới: lịch hẹn được TẠO trong {@code [tu, den)}, trừ DA_HUY_DO_DOI_LICH. */
    @Query("""
            select count(l) from LichHen l
            where l.ngayTao >= :tu and l.ngayTao < :den
              and l.trangThai <> iuh.fit.se.eclinic.common.enums.TrangThaiLichHen.DA_HUY_DO_DOI_LICH
              and (:idChuyenKhoa is null or l.bacSi.chuyenKhoa.id = :idChuyenKhoa)
            """)
    long demDatMoi(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa);

}
