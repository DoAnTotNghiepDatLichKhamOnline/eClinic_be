package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.LichLamViecProperties;
import iuh.fit.se.eclinic.booking.config.PhieuKhamProperties;
import iuh.fit.se.eclinic.booking.repository.SuKienThongBaoRepository;
import iuh.fit.se.eclinic.booking.repository.TaiKhoanChiDocRepository;
import iuh.fit.se.eclinic.booking.service.LyDoHuyCa;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichLamViecService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.SuKienThongBao;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.KenhThongBao;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import iuh.fit.se.eclinic.common.enums.LoaiYeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.thongbao.ThamSoThongBao;
import lombok.RequiredArgsConstructor;

/**
 * Như {@link ThongBaoLichHenServiceImpl}: ghi khoá mẫu + tham số, không soạn câu. Tên phòng, tầng là tham số 2 ngôn ngữ
 * (chụp lại lúc xảy ra sự kiện); 1 ca viết bằng 3 tham số giờ bắt đầu, giờ kết thúc, ngày.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ThongBaoLichLamViecServiceImpl implements ThongBaoLichLamViecService {

    private final SuKienThongBaoRepository suKienThongBaoRepository;
    private final TaiKhoanChiDocRepository taiKhoanChiDocRepository;
    private final PhieuKhamProperties phieuKhamProperties;
    private final LichLamViecProperties lichLamViecProperties;
    private final ThongBaoLichHenService thongBaoLichHenService;

    @Override
    public void yeuCauMoi(YeuCauDoiLich yeuCau) {
        ThamSoThongBao thamSo = ThamSoThongBao.tao().chu(hoTen(yeuCau.getBacSi()));
        String khoaMau;
        if (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.NHUONG_CA) {
            khoaMau = "thongBao.yeuCauMoi.nhuongCa";
            thamSo.chu(hoTen(yeuCau.getBacSiNhan()));
        } else {
            khoaMau = "thongBao.yeuCauMoi." + (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.XIN_NGHI ? "xinNghi" : "xinDoi");
        }
        baoQuanTri(yeuCau.getId(), khoaMau, ca(thamSo, yeuCau.getLichLamViec()));
    }

    @Override
    public void yeuCauMoiNhieuCa(List<YeuCauDoiLich> cacYeuCau) {
        YeuCauDoiLich dau = cacYeuCau.get(0);
        baoQuanTri(dau.getId(), "thongBao.yeuCauMoi.xinNghiNhieuCa",
                khoangNgay(ThamSoThongBao.tao().chu(hoTen(dau.getBacSi())), cacYeuCau));
    }

    @Override
    public void ketQuaYeuCau(YeuCauDoiLich yeuCau) {
        String ghiChu = yeuCau.getGhiChuXuLy();
        boolean duyet = yeuCau.getTrangThai() == TrangThaiYeuCau.DA_DUYET;
        String khoaMau = "thongBao.ketQuaYeuCau." + loai(yeuCau) + (duyet ? ".duyet" : ".tuChoi")
                + (ghiChu == null ? "" : ".coGhiChu");
        ThamSoThongBao thamSo = ca(ThamSoThongBao.tao(), yeuCau.getLichLamViec());
        if (ghiChu != null) {
            thamSo.chu(ghiChu);
        }
        baoNguoiGui(yeuCau, khoaMau, thamSo);
        if (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.NHUONG_CA) {
            baoBacSiNhan(yeuCau, "thongBao.yeuCauNhuongCa." + (duyet ? "duyet" : "tuChoi"), nguoiGuiVaCa(yeuCau));
        }
    }

    @Override
    public void ketQuaNhom(List<YeuCauDoiLich> daXuLy) {
        YeuCauDoiLich dau = daXuLy.get(0);
        String ghiChu = dau.getGhiChuXuLy();
        String khoaMau = "thongBao.ketQuaYeuCau.xinNghiNhieuCa"
                + (dau.getTrangThai() == TrangThaiYeuCau.DA_DUYET ? ".duyet" : ".tuChoi")
                + (ghiChu == null ? "" : ".coGhiChu");
        ThamSoThongBao thamSo = khoangNgay(ThamSoThongBao.tao(), daXuLy);
        if (ghiChu != null) {
            thamSo.chu(ghiChu);
        }
        baoNguoiGui(dau, khoaMau, thamSo);
    }

    @Override
    public void yeuCauDongDoHuyCa(YeuCauDoiLich yeuCau, LyDoHuyCa lyDo) {
        baoNguoiGui(yeuCau, "thongBao.ketQuaYeuCau." + loai(yeuCau) + ".caBiHuy",
                lyDo.themVao(ca(ThamSoThongBao.tao(), yeuCau.getLichLamViec())));
        if (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.NHUONG_CA) {
            baoBacSiNhan(yeuCau, "thongBao.yeuCauNhuongCa.caBiHuy", lyDo.themVao(nguoiGuiVaCa(yeuCau)));
        }
    }

    @Override
    public void nhuongCaMoi(YeuCauDoiLich yeuCau) {
        baoBacSiNhan(yeuCau, "thongBao.yeuCauNhuongCa.moi", nguoiGuiVaCa(yeuCau));
    }

    @Override
    public void nhuongCaDaRut(YeuCauDoiLich yeuCau) {
        baoBacSiNhan(yeuCau, "thongBao.yeuCauNhuongCa.daRut", nguoiGuiVaCa(yeuCau));
    }

    @Override
    public void dongNghiepPhanHoi(YeuCauDoiLich yeuCau) {
        boolean dongY = yeuCau.getTrangThai() == TrangThaiYeuCau.CHO_DUYET;
        String ghiChu = yeuCau.getGhiChuBacSiNhan();
        ThamSoThongBao thamSo = ca(ThamSoThongBao.tao().chu(hoTen(yeuCau.getBacSiNhan())), yeuCau.getLichLamViec());
        String khoaMau = "thongBao.ketQuaYeuCau.nhuongCa.dongY";
        if (!dongY) {
            khoaMau = "thongBao.ketQuaYeuCau.nhuongCa.tuChoiNhan" + (ghiChu == null ? "" : ".coGhiChu");
            if (ghiChu != null) {
                thamSo.chu(ghiChu);
            }
        }
        baoNguoiGui(yeuCau, khoaMau, thamSo);
    }

    @Override
    public void lichHenDoiBacSi(LichHen lichHen, BacSi bacSiCu) {
        ThamSoThongBao thamSo = ThamSoThongBao.tao().chu(lichHen.getMaTraCuu())
                .gioNgay(lichHen.getKhungGio().getGioBatDau()).chu(hoTen(bacSiCu)).chu(hoTen(lichHen.getBacSi()));
        for (Long idTaiKhoan : ThongBaoLichHenServiceImpl.taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_DOI_BAC_SI, idTaiKhoan, lichHen.getId(), null, "thongBao.lichHenDoiBacSi",
                    thamSo);
        }
        thongBaoLichHenService.emailDoiBacSi(lichHen);
    }

    /** Mọi quản trị viên đang hoạt động. */
    private void baoQuanTri(Long idYeuCau, String khoaMau, ThamSoThongBao thamSo) {
        for (Long idTaiKhoan : taiKhoanChiDocRepository.timIdTheoVaiTroVaTrangThai(VaiTro.QUAN_TRI_VIEN,
                TrangThaiTaiKhoan.DA_KICH_HOAT)) {
            ghi(LoaiThongBao.YEU_CAU_DOI_LICH_MOI, idTaiKhoan, null, idYeuCau, khoaMau, thamSo);
        }
    }

    /** Bác sĩ đã gửi yêu cầu. */
    private void baoNguoiGui(YeuCauDoiLich yeuCau, String khoaMau, ThamSoThongBao thamSo) {
        ghi(LoaiThongBao.KET_QUA_DUYET_DOI_LICH, yeuCau.getBacSi().getTaiKhoan().getId(), null, yeuCau.getId(), khoaMau,
                thamSo);
    }

    /** Bác sĩ nhận ca của yêu cầu nhường ca. */
    private void baoBacSiNhan(YeuCauDoiLich yeuCau, String khoaMau, ThamSoThongBao thamSo) {
        ghi(LoaiThongBao.YEU_CAU_NHUONG_CA, yeuCau.getBacSiNhan().getTaiKhoan().getId(), null, yeuCau.getId(),
                khoaMau, thamSo);
    }

    /** Họ tên bác sĩ gửi rồi 3 tham số của ca (mẫu cho bác sĩ nhận: "{0} ... {1}-{2} ngày {3}"). */
    private static ThamSoThongBao nguoiGuiVaCa(YeuCauDoiLich yeuCau) {
        return ca(ThamSoThongBao.tao().chu(hoTen(yeuCau.getBacSi())), yeuCau.getLichLamViec());
    }

    /** Số yêu cầu, ngày của ca đầu, ngày của ca cuối (các yêu cầu theo ngày của ca). */
    private static ThamSoThongBao khoangNgay(ThamSoThongBao thamSo, List<YeuCauDoiLich> cacYeuCau) {
        return thamSo.so(cacYeuCau.size()).ngay(cacYeuCau.get(0).getLichLamViec().getNgayLamViec())
                .ngay(cacYeuCau.get(cacYeuCau.size() - 1).getLichLamViec().getNgayLamViec());
    }

    /** Phần loại yêu cầu trong khoá mẫu thongBao.ketQuaYeuCau.*. */
    private static String loai(YeuCauDoiLich yeuCau) {
        return switch (yeuCau.getLoaiYeuCau()) {
            case XIN_NGHI -> "xinNghi";
            case DOI_CA -> "doi";
            case NHUONG_CA -> "nhuongCa";
        };
    }

    /** Chỉ họ tên; chữ "Bác sĩ" / "Dr." nằm trong mẫu của từng ngôn ngữ. */
    private static String hoTen(BacSi bacSi) {
        return bacSi.getTaiKhoan().getHoTen();
    }

    @Override
    public void caMoi(BacSi bacSi, List<LichLamViec> daTao, PhongKham phongKham) {
        LichLamViec dau = daTao.get(0);
        if (daTao.size() == 1) {
            ghiChoBacSi(bacSi, "thongBao.caMoi", tenPhong(ca(ThamSoThongBao.tao(), dau), phongKham));
            return;
        }
        ThamSoThongBao thamSo = ca(ca(ThamSoThongBao.tao().so(daTao.size()), dau), daTao.get(daTao.size() - 1));
        ghiChoBacSi(bacSi, "thongBao.nhieuCaMoi", tenPhong(thamSo, phongKham));
    }

    @Override
    public void caMoiHangLoat(BacSi bacSi, List<LichLamViec> daTao) {
        PhongKham phongKham = daTao.get(0).getPhongKham();
        if (daTao.stream().allMatch(ca -> ca.getPhongKham().getId().equals(phongKham.getId()))) {
            caMoi(bacSi, daTao, phongKham);
            return;
        }
        ghiChoBacSi(bacSi, "thongBao.nhieuCaMoi.nhieuPhong",
                ca(ca(ThamSoThongBao.tao().so(daTao.size()), daTao.get(0)), daTao.get(daTao.size() - 1)));
    }

    @Override
    public void caDaSua(LichLamViec ca) {
        ghiChoBacSi(ca.getBacSi(), "thongBao.caDaSua", tenPhong(ca(ThamSoThongBao.tao(), ca), ca.getPhongKham()));
    }

    @Override
    public void caBiHuy(LichLamViec ca, LyDoHuyCa lyDo) {
        ghiChoBacSi(ca.getBacSi(), "thongBao.caBiHuy", lyDo.themVao(ca(ThamSoThongBao.tao(), ca)));
    }

    /**
     * Thứ tự tham số của mẫu email.lichLamViec (notification-service: {@code MauEmail}): họ tên bác sĩ, từ ngày, đến
     * ngày, tổng số ca, số ca không liệt kê, link trang Lịch làm việc; rồi mỗi ca 4 tham số: ngày, giờ bắt đầu, giờ kết
     * thúc, phòng (kèm tầng, 2 ngôn ngữ).
     */
    @Override
    public void guiLich(BacSi bacSi, LocalDate tuNgay, LocalDate denNgay, List<LichLamViec> cacCa) {
        TaiKhoan taiKhoan = bacSi.getTaiKhoan();
        ghiChoBacSi(bacSi, "thongBao.lichLamViecDaGui",
                ThamSoThongBao.tao().ngay(tuNgay).ngay(denNgay).so(cacCa.size()));
        int soDong = Math.min(cacCa.size(), SO_CA_TOI_DA_TRONG_EMAIL);
        ThamSoThongBao thamSo = ThamSoThongBao.tao().chu(taiKhoan.getHoTen()).ngay(tuNgay).ngay(denNgay)
                .so(cacCa.size()).so(cacCa.size() - soDong)
                .chu(phieuKhamProperties.frontendUrl() + lichLamViecProperties.duongDanLichBacSi() + "?tuNgay="
                        + tuNgay + "&denNgay=" + denNgay);
        for (LichLamViec ca : cacCa.subList(0, soDong)) {
            PhongKham phong = ca.getPhongKham();
            thamSo.ngay(ca.getNgayLamViec()).gio(ca.getGioBatDau()).gio(ca.getGioKetThuc())
                    .haiNgonNgu(phongKemTang(phong, NgonNgu.VI), phongKemTang(phong, NgonNgu.EN));
        }
        SuKienThongBao email = new SuKienThongBao();
        email.setKenh(KenhThongBao.EMAIL);
        email.setEmailNhan(taiKhoan.getEmail());
        email.setNgonNgu(taiKhoan.getNgonNgu() == null ? NgonNgu.VI : taiKhoan.getNgonNgu());
        email.setKhoaMau("email.lichLamViec");
        email.setThamSo(thamSo.toJson());
        suKienThongBaoRepository.save(email);
    }

    /** "Phòng 101 · Tầng 1"; phòng không ghi tầng thì chỉ có tên phòng. */
    private static String phongKemTang(PhongKham phong, NgonNgu ngonNgu) {
        String tang = phong.tang(ngonNgu);
        return tang == null || tang.isBlank() ? phong.ten(ngonNgu) : phong.ten(ngonNgu) + " · " + tang;
    }

    @Override
    public void lichHenCanDoi(LichHen lichHen, LyDoHuyCa lyDo) {
        ThamSoThongBao thamSo = lyDo.themVao(ThamSoThongBao.tao().chu(lichHen.getMaTraCuu())
                .gioNgay(lichHen.getKhungGio().getGioBatDau()));
        for (Long idTaiKhoan : ThongBaoLichHenServiceImpl.taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_CAN_DOI, idTaiKhoan, lichHen.getId(), null, "thongBao.lichHenCanDoi", thamSo);
        }
    }

    @Override
    public void lichHenDoiPhong(LichHen lichHen) {
        PhongKham phong = lichHen.getPhongKham();
        ThamSoThongBao thamSo = tenPhong(ThamSoThongBao.tao().chu(lichHen.getMaTraCuu())
                .gioNgay(lichHen.getKhungGio().getGioBatDau()), phong);
        String khoaMau = "thongBao.lichHenDoiPhong";
        if (phong.getTang() != null) {
            khoaMau += ".coTang";
            thamSo.haiNgonNgu(phong.getTang(), phong.getTangEn());
        }
        for (Long idTaiKhoan : ThongBaoLichHenServiceImpl.taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_DOI_PHONG, idTaiKhoan, lichHen.getId(), null, khoaMau, thamSo);
        }
    }

    /** 3 tham số của 1 ca: giờ bắt đầu, giờ kết thúc, ngày (mẫu viết "{n}-{n+1} ngày {n+2}"). */
    private static ThamSoThongBao ca(ThamSoThongBao thamSo, LichLamViec ca) {
        return thamSo.gio(ca.getGioBatDau()).gio(ca.getGioKetThuc()).ngay(ca.getNgayLamViec());
    }

    private static ThamSoThongBao tenPhong(ThamSoThongBao thamSo, PhongKham phongKham) {
        return thamSo.haiNgonNgu(phongKham.getTenPhong(), phongKham.getTenPhongEn());
    }

    private void ghiChoBacSi(BacSi bacSi, String khoaMau, ThamSoThongBao thamSo) {
        ghi(LoaiThongBao.CA_LAM_VIEC_THAY_DOI, bacSi.getTaiKhoan().getId(), null, null, khoaMau, thamSo);
    }

    private void ghi(LoaiThongBao loai, Long idTaiKhoan, Long idLichHen, Long idYeuCau, String khoaMau,
            ThamSoThongBao thamSo) {
        SuKienThongBao suKien = new SuKienThongBao();
        suKien.setLoai(loai);
        suKien.setIdTaiKhoan(idTaiKhoan);
        suKien.setIdLichHen(idLichHen);
        suKien.setIdYeuCau(idYeuCau);
        suKien.setKhoaMau(khoaMau);
        suKien.setThamSo(thamSo.toJson());
        suKienThongBaoRepository.save(suKien);
    }
}
