package iuh.fit.se.eclinic.booking.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.SuKienThongBaoRepository;
import iuh.fit.se.eclinic.booking.repository.TaiKhoanChiDocRepository;
import iuh.fit.se.eclinic.booking.service.LyDoHuyCa;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichLamViecService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.SuKienThongBao;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import iuh.fit.se.eclinic.common.enums.LoaiYeuCauDoiLich;
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

    @Override
    public void yeuCauMoi(YeuCauDoiLich yeuCau) {
        String khoaMau = "thongBao.yeuCauMoi." + (xinNghi(yeuCau) ? "xinNghi" : "xinDoi");
        ThamSoThongBao thamSo = ca(ThamSoThongBao.tao().chu(yeuCau.getBacSi().getTaiKhoan().getHoTen()),
                yeuCau.getLichLamViec());
        for (Long idTaiKhoan : taiKhoanChiDocRepository.timIdTheoVaiTroVaTrangThai(VaiTro.QUAN_TRI_VIEN,
                TrangThaiTaiKhoan.DA_KICH_HOAT)) {
            ghi(LoaiThongBao.YEU_CAU_DOI_LICH_MOI, idTaiKhoan, null, yeuCau.getId(), khoaMau, thamSo);
        }
    }

    @Override
    public void ketQuaYeuCau(YeuCauDoiLich yeuCau) {
        String ghiChu = yeuCau.getGhiChuXuLy();
        String khoaMau = "thongBao.ketQuaYeuCau." + (xinNghi(yeuCau) ? "xinNghi" : "doi")
                + (yeuCau.getTrangThai() == TrangThaiYeuCau.DA_DUYET ? ".duyet" : ".tuChoi")
                + (ghiChu == null ? "" : ".coGhiChu");
        ThamSoThongBao thamSo = ca(ThamSoThongBao.tao(), yeuCau.getLichLamViec());
        if (ghiChu != null) {
            thamSo.chu(ghiChu);
        }
        ghi(LoaiThongBao.KET_QUA_DUYET_DOI_LICH, yeuCau.getBacSi().getTaiKhoan().getId(), null, yeuCau.getId(),
                khoaMau, thamSo);
    }

    @Override
    public void yeuCauDongDoHuyCa(YeuCauDoiLich yeuCau, LyDoHuyCa lyDo) {
        ghi(LoaiThongBao.KET_QUA_DUYET_DOI_LICH, yeuCau.getBacSi().getTaiKhoan().getId(), null, yeuCau.getId(),
                "thongBao.ketQuaYeuCau." + (xinNghi(yeuCau) ? "xinNghi" : "doi") + ".caBiHuy",
                lyDo.themVao(ca(ThamSoThongBao.tao(), yeuCau.getLichLamViec())));
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
    public void caDaSua(LichLamViec ca) {
        ghiChoBacSi(ca.getBacSi(), "thongBao.caDaSua", tenPhong(ca(ThamSoThongBao.tao(), ca), ca.getPhongKham()));
    }

    @Override
    public void caBiHuy(LichLamViec ca, LyDoHuyCa lyDo) {
        ghiChoBacSi(ca.getBacSi(), "thongBao.caBiHuy", lyDo.themVao(ca(ThamSoThongBao.tao(), ca)));
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

    private static boolean xinNghi(YeuCauDoiLich yeuCau) {
        return yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.XIN_NGHI;
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
