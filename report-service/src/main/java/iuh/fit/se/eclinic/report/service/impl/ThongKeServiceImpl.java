package iuh.fit.se.eclinic.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.util.MaBacSi;
import iuh.fit.se.eclinic.report.dto.request.KyBaoCao;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.ChiSo;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.DiemThoiGian;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.DongBacSi;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.DongChuyenKhoa;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.KenhDat;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.ThoiGianDatTruoc;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.TongQuan;
import iuh.fit.se.eclinic.report.dto.response.BaoCaoThongKeResponse.TrangThai;
import iuh.fit.se.eclinic.report.repository.DanhMucChiDocRepository;
import iuh.fit.se.eclinic.report.repository.DanhMucChiDocRepository.BacSiTomTat;
import iuh.fit.se.eclinic.report.repository.ThongKeCaRepository;
import iuh.fit.se.eclinic.report.repository.ThongKeLichHenRepository;
import iuh.fit.se.eclinic.report.repository.ThongKeLichHenRepository.SoLuongTheoNhom;
import iuh.fit.se.eclinic.report.service.KhoangBaoCao;
import iuh.fit.se.eclinic.report.service.ThongKeService;
import lombok.RequiredArgsConstructor;

/** Vài truy vấn gom nhóm theo bác sĩ, ngày và trạng thái, rồi cộng lên chuyên khoa, toàn viện. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThongKeServiceImpl implements ThongKeService {

    private static final BigDecimal TRAM = BigDecimal.valueOf(100);
    private static final BigDecimal SAU_MUOI = BigDecimal.valueOf(60);

    private final ThongKeLichHenRepository lichHenRepository;
    private final ThongKeCaRepository caRepository;
    private final DanhMucChiDocRepository danhMucRepository;

    @Override
    public BaoCaoThongKeResponse lap(KyBaoCao ky, LocalDate ngay, LocalDate tuNgay, LocalDate denNgay,
            Long idChuyenKhoa) {
        KhoangBaoCao khoang = KhoangBaoCao.tu(ky, ngay, tuNgay, denNgay);
        List<ChuyenKhoa> cacChuyenKhoa = idChuyenKhoa == null ? danhMucRepository.timChuyenKhoa()
                : List.of(danhMucRepository.timChuyenKhoa(idChuyenKhoa)
                        .orElseThrow(() -> new LoiKhongTimThay("ChuyenKhoa", idChuyenKhoa)));
        LocalDateTime tu = khoang.tuNgay().atStartOfDay();
        LocalDateTime den = khoang.denNgay().plusDays(1).atStartOfDay();
        NgonNgu ngonNgu = NgonNgu.hienTai();
        Collator thuTu = Collator.getInstance(ngonNgu.locale());

        BoDem toanVien = new BoDem();
        Map<Long, BoDem> theoBacSi = new HashMap<>();
        lichHenRepository.demTheoBacSiVaTrangThai(tu, den, idChuyenKhoa).forEach(dong -> {
            theoBacSi.computeIfAbsent(dong.getIdBacSi(), id -> new BoDem()).them(dong.getTrangThai(), dong.getSoLuong());
            toanVien.them(dong.getTrangThai(), dong.getSoLuong());
        });
        caRepository.demLuotTheoBacSi(khoang.tuNgay(), khoang.denNgay(), idChuyenKhoa).forEach(dong -> {
            theoBacSi.computeIfAbsent(dong.getIdBacSi(), id -> new BoDem()).themLuot(dong.getTrangThai(),
                    dong.getSoLuong());
            toanVien.themLuot(dong.getTrangThai(), dong.getSoLuong());
        });
        ganBenhNhan(theoBacSi, lichHenRepository.demBenhNhanDaKhamTheoBacSi(tu, den, idChuyenKhoa));
        toanVien.benhNhan = lichHenRepository.demBenhNhanDaKham(tu, den, idChuyenKhoa);

        Map<Long, BacSiTomTat> bacSi = theoBacSi.isEmpty() ? Map.of()
                : danhMucRepository.timBacSi(theoBacSi.keySet()).stream()
                        .collect(Collectors.toMap(BacSiTomTat::getId, Function.identity()));
        Map<Long, BoDem> theoChuyenKhoa = new HashMap<>();
        theoBacSi.forEach((id, dem) -> theoChuyenKhoa
                .computeIfAbsent(bacSi.get(id).getIdChuyenKhoa(), ck -> new BoDem()).cong(dem));
        ganBenhNhan(theoChuyenKhoa, lichHenRepository.demBenhNhanDaKhamTheoChuyenKhoa(tu, den, idChuyenKhoa));

        Map<Long, String> tenChuyenKhoa = new HashMap<>();
        cacChuyenKhoa.forEach(ck -> tenChuyenKhoa.put(ck.getId(), ngonNgu.chon(ck.getTenChuyenKhoa(),
                ck.getTenChuyenKhoaEn())));
        List<DongChuyenKhoa> dongChuyenKhoa = cacChuyenKhoa.stream()
                .map(ck -> new DongChuyenKhoa(ck.getId(), tenChuyenKhoa.get(ck.getId()),
                        theoChuyenKhoa.getOrDefault(ck.getId(), new BoDem()).chiSo()))
                .sorted(Comparator.comparing(DongChuyenKhoa::tenChuyenKhoa, thuTu))
                .toList();
        List<DongBacSi> dongBacSi = theoBacSi.entrySet().stream()
                .map(muc -> {
                    BacSiTomTat bs = bacSi.get(muc.getKey());
                    return new DongBacSi(bs.getId(), MaBacSi.tu(bs.getId()), bs.getHoTen(), bs.getIdChuyenKhoa(),
                            tenChuyenKhoa.get(bs.getIdChuyenKhoa()), muc.getValue().chiSo());
                })
                .sorted(Comparator.comparingLong((DongBacSi d) -> d.chiSo().lichHen()).reversed()
                        .thenComparing(DongBacSi::hoTen, Comparator.nullsLast(thuTu)))
                .toList();

        TongQuan tongQuan = new TongQuan(danhMucRepository.demBacSiDangCongTac(idChuyenKhoa),
                caRepository.demBacSiCoCa(khoang.tuNgay(), khoang.denNgay(), idChuyenKhoa),
                danhMucRepository.demTaiKhoanBenhNhanMoi(tu, den), toanVien.benhNhan,
                toanVien.dem(TrangThaiLichHen.DA_HOAN_THANH), toanVien.lichHen(),
                lichHenRepository.demDatMoi(tu, den, idChuyenKhoa), toanVien.tyLeHuy(), toanVien.tyLeKhongDen(),
                toanVien.luotDaDat, toanVien.tongLuot, tyLe(toanVien.luotDaDat, toanVien.tongLuot));
        TrangThai trangThai = new TrangThai(toanVien.dem(TrangThaiLichHen.DA_XAC_NHAN),
                toanVien.dem(TrangThaiLichHen.DA_HOAN_THANH), toanVien.dem(TrangThaiLichHen.KHONG_DEN),
                toanVien.dem(TrangThaiLichHen.DA_HUY), toanVien.dem(TrangThaiLichHen.BI_TU_CHOI),
                toanVien.dem(TrangThaiLichHen.QUAN_TRI_HUY), lichHenRepository.demCanDoiLich(tu, den, idChuyenKhoa));

        return new BaoCaoThongKeResponse(khoang.ky(), khoang.tuNgay(), khoang.denNgay(), idChuyenKhoa, khoang.donVi(),
                tongQuan, trangThai, kenhDat(tu, den, idChuyenKhoa), thoiGianDatTruoc(tu, den, idChuyenKhoa),
                dongChuyenKhoa, dongBacSi, theoThoiGian(khoang, tu, den, idChuyenKhoa), LocalDateTime.now());
    }

    private KenhDat kenhDat(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa) {
        Map<KenhDatLich, Long> theoKenh = new EnumMap<>(KenhDatLich.class);
        lichHenRepository.demTheoKenh(tu, den, idChuyenKhoa)
                .forEach(dong -> theoKenh.put(dong.getKenhDat(), dong.getSoLuong()));
        return new KenhDat(theoKenh.getOrDefault(KenhDatLich.KHACH, 0L),
                theoKenh.getOrDefault(KenhDatLich.TAI_KHOAN, 0L), theoKenh.getOrDefault(KenhDatLich.HOTLINE, 0L));
    }

    private ThoiGianDatTruoc thoiGianDatTruoc(LocalDateTime tu, LocalDateTime den, Long idChuyenKhoa) {
        ThongKeLichHenRepository.ThoiGianDatTruoc dong = lichHenRepository.thoiGianDatTruoc(tu, den, idChuyenKhoa);
        BigDecimal trungBinhGio = dong.getTrungBinhPhut() == null ? null
                : BigDecimal.valueOf(dong.getTrungBinhPhut()).divide(SAU_MUOI, 1, RoundingMode.HALF_UP);
        return new ThoiGianDatTruoc(trungBinhGio, khongNull(dong.getDuoi1Ngay()), khongNull(dong.getTu1Den3Ngay()),
                khongNull(dong.getTu4Den7Ngay()), khongNull(dong.getTu8Den14Ngay()), khongNull(dong.getTren14Ngay()));
    }

    /** Đủ mọi điểm từ đầu tới cuối kỳ; điểm đầu / cuối cắt theo kỳ (vd tuần đầu bắt đầu giữa tuần). */
    private List<DiemThoiGian> theoThoiGian(KhoangBaoCao khoang, LocalDateTime tu, LocalDateTime den,
            Long idChuyenKhoa) {
        Map<LocalDate, BoDem> theoDiem = new LinkedHashMap<>();
        for (LocalDate dau = khoang.dauDiem(khoang.tuNgay()); !dau.isAfter(khoang.denNgay());
                dau = khoang.dauDiemSau(dau)) {
            theoDiem.put(dau, new BoDem());
        }
        lichHenRepository.demTheoNgayVaTrangThai(tu, den, idChuyenKhoa)
                .forEach(dong -> theoDiem.get(khoang.dauDiem(dong.getNgay())).them(dong.getTrangThai(),
                        dong.getSoLuong()));
        List<DiemThoiGian> chuoi = new ArrayList<>(theoDiem.size());
        theoDiem.forEach((dau, dem) -> {
            LocalDate tuNgay = dau.isBefore(khoang.tuNgay()) ? khoang.tuNgay() : dau;
            LocalDate cuoi = khoang.dauDiemSau(dau).minusDays(1);
            LocalDate denNgay = cuoi.isAfter(khoang.denNgay()) ? khoang.denNgay() : cuoi;
            chuoi.add(new DiemThoiGian(tuNgay, denNgay, dem.lichHen(), dem.dem(TrangThaiLichHen.DA_HOAN_THANH),
                    dem.daHuy(), dem.dem(TrangThaiLichHen.KHONG_DEN)));
        });
        return chuoi;
    }

    private static void ganBenhNhan(Map<Long, BoDem> theoNhom, List<SoLuongTheoNhom> cacDong) {
        cacDong.forEach(dong -> theoNhom.computeIfAbsent(dong.getId(), id -> new BoDem()).benhNhan = dong.getSoLuong());
    }

    private static long khongNull(Long so) {
        return so == null ? 0 : so;
    }

    /** Phần trăm, 1 chữ số thập phân; null khi mẫu số bằng 0. */
    static BigDecimal tyLe(long tu, long mau) {
        return mau == 0 ? null : BigDecimal.valueOf(tu).multiply(TRAM).divide(BigDecimal.valueOf(mau), 1,
                RoundingMode.HALF_UP);
    }

    /** Bộ đếm của 1 nhóm (toàn viện, 1 chuyên khoa, 1 bác sĩ, 1 điểm thời gian). */
    private static final class BoDem {

        private final Map<TrangThaiLichHen, Long> theoTrangThai = new EnumMap<>(TrangThaiLichHen.class);
        private long luotDaDat;
        private long tongLuot;
        private long benhNhan;

        void them(TrangThaiLichHen trangThai, long soLuong) {
            theoTrangThai.merge(trangThai, soLuong, Long::sum);
        }

        void themLuot(TrangThaiKhungGio trangThai, long soLuong) {
            tongLuot += soLuong;
            if (trangThai == TrangThaiKhungGio.DA_DAT) {
                luotDaDat += soLuong;
            }
        }

        /** Cộng số của 1 nhóm con (trừ số bệnh nhân duy nhất: không cộng được, đếm riêng). */
        void cong(BoDem con) {
            con.theoTrangThai.forEach(this::them);
            luotDaDat += con.luotDaDat;
            tongLuot += con.tongLuot;
        }

        long dem(TrangThaiLichHen trangThai) {
            return theoTrangThai.getOrDefault(trangThai, 0L);
        }

        long lichHen() {
            return theoTrangThai.values().stream().mapToLong(Long::longValue).sum();
        }

        long daHuy() {
            return dem(TrangThaiLichHen.DA_HUY) + dem(TrangThaiLichHen.BI_TU_CHOI)
                    + dem(TrangThaiLichHen.QUAN_TRI_HUY);
        }

        BigDecimal tyLeHuy() {
            return tyLe(daHuy(), lichHen());
        }

        BigDecimal tyLeKhongDen() {
            long khongDen = dem(TrangThaiLichHen.KHONG_DEN);
            return tyLe(khongDen, khongDen + dem(TrangThaiLichHen.DA_HOAN_THANH));
        }

        ChiSo chiSo() {
            return new ChiSo(lichHen(), dem(TrangThaiLichHen.DA_HOAN_THANH), benhNhan, daHuy(),
                    dem(TrangThaiLichHen.KHONG_DEN), tyLeHuy(), tyLeKhongDen(), luotDaDat, tongLuot,
                    tyLe(luotDaDat, tongLuot));
        }
    }

}
