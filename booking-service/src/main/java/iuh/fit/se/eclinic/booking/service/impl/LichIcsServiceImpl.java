package iuh.fit.se.eclinic.booking.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.booking.config.PhieuKhamProperties;
import iuh.fit.se.eclinic.booking.service.LichIcsService;
import iuh.fit.se.eclinic.booking.util.TepLichIcs;
import iuh.fit.se.eclinic.booking.util.TepLichIcs.TrangThai;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LichIcsServiceImpl implements LichIcsService {

    /** Lịch hẹn không còn diễn ra: hủy (bởi ai cũng vậy) hoặc đã đổi sang lịch hẹn khác. */
    private static final Set<TrangThaiLichHen> DA_HUY = EnumSet.of(TrangThaiLichHen.DA_HUY,
            TrangThaiLichHen.BI_TU_CHOI, TrangThaiLichHen.QUAN_TRI_HUY, TrangThaiLichHen.DA_HUY_DO_DOI_LICH);
    private static final int SO_GIAI_DOAN = 4;
    private static final int GIAI_DOAN_DOI_BAC_SI = 1;
    private static final int GIAI_DOAN_CHO_DOI_LICH = 2;
    private static final int GIAI_DOAN_DA_HUY = 3;
    private static final String DUOI_UID = "@eclinic";

    private final MessageSource messageSource;
    private final PhieuKhamProperties phieuKhamProperties;

    @Override
    public String tao(LichHen lichHen, NgonNgu ngonNgu) {
        Locale locale = ngonNgu.locale();
        boolean daHuy = DA_HUY.contains(lichHen.getTrangThai());
        int giaiDoan;
        TrangThai trangThai;
        if (daHuy) {
            giaiDoan = GIAI_DOAN_DA_HUY;
            trangThai = TrangThai.CANCELLED;
        } else if (lichHen.isCanDoiLich()) {
            giaiDoan = GIAI_DOAN_CHO_DOI_LICH;
            trangThai = TrangThai.TENTATIVE;
        } else {
            giaiDoan = lichHen.isDoiBacSi() ? GIAI_DOAN_DOI_BAC_SI : 0;
            trangThai = TrangThai.CONFIRMED;
        }
        // Đi ngược chuỗi đổi lịch tới lịch hẹn đầu tiên: UID chung của cả chuỗi
        LichHen goc = lichHen;
        int soLanDoi = 0;
        while (goc.getLichHenCu() != null) {
            goc = goc.getLichHenCu();
            soLanDoi++;
        }
        String uid = (goc.getMaTraCuu() != null ? goc.getMaTraCuu() : "lich-hen-" + goc.getId()) + DUOI_UID;

        String lienKet = phieuKhamProperties.lienKet(lichHen.getMaTokenPhieuKham());
        List<String> moTa = new ArrayList<>();
        moTa.add(chu("lichIcs.moTa.maTraCuu", locale, lichHen.getMaTraCuu()));
        if (lichHen.getSoThuTu() != null) {
            moTa.add(chu("lichIcs.moTa.soThuTu", locale, lichHen.getSoThuTu()));
        }
        moTa.add(chu("lichIcs.moTa.chuyenKhoa", locale, lichHen.getBacSi().getChuyenKhoa().ten(ngonNgu)));
        moTa.add(chu("lichIcs.moTa.phieuKham", locale, lienKet));
        if (daHuy) {
            moTa.add(chu("lichIcs.moTa.daHuy", locale));
        } else if (lichHen.isCanDoiLich()) {
            moTa.add(chu("lichIcs.moTa.canDoiLich", locale));
        }

        String tenBacSi = lichHen.getBacSi().getTaiKhoan().getHoTen();
        TepLichIcs.SuKien suKien = new TepLichIcs.SuKien(uid, SO_GIAI_DOAN * soLanDoi + giaiDoan, trangThai,
                lichHen.getKhungGio().getGioBatDau(), lichHen.getKhungGio().getGioKetThuc(),
                chu(daHuy ? "lichIcs.tieuDe.daHuy" : "lichIcs.tieuDe", locale, tenBacSi),
                diaDiem(lichHen.getPhongKham(), ngonNgu, locale), moTa, lienKet, chu("lichIcs.nhac", locale));
        return TepLichIcs.tao(suKien, Instant.now());
    }

    private String diaDiem(PhongKham phong, NgonNgu ngonNgu, Locale locale) {
        String tenPhong = ngonNgu.chon(phong.getTenPhong(), phong.getTenPhongEn());
        String tang = ngonNgu.chon(phong.getTang(), phong.getTangEn());
        return tang == null || tang.isBlank() ? chu("lichIcs.diaDiem", locale, tenPhong)
                : chu("lichIcs.diaDiem.coTang", locale, tenPhong, tang);
    }

    private String chu(String khoa, Locale locale, Object... thamSo) {
        return messageSource.getMessage(khoa, thamSo, khoa, locale);
    }

}
