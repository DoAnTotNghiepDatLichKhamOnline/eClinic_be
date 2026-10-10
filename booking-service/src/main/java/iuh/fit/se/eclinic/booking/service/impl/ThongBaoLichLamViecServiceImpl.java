package iuh.fit.se.eclinic.booking.service.impl;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.SuKienThongBaoRepository;
import iuh.fit.se.eclinic.booking.repository.TaiKhoanChiDocRepository;
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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ThongBaoLichLamViecServiceImpl implements ThongBaoLichLamViecService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final SuKienThongBaoRepository suKienThongBaoRepository;
    private final TaiKhoanChiDocRepository taiKhoanChiDocRepository;

    @Override
    public void yeuCauMoi(YeuCauDoiLich yeuCau) {
        String noiDung = "Bác sĩ " + yeuCau.getBacSi().getTaiKhoan().getHoTen() + " "
                + (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.XIN_NGHI ? "xin nghỉ" : "xin đổi") + " ca "
                + moTaCa(yeuCau.getLichLamViec()) + ".";
        for (Long idTaiKhoan : taiKhoanChiDocRepository.timIdTheoVaiTroVaTrangThai(VaiTro.QUAN_TRI_VIEN,
                TrangThaiTaiKhoan.DA_KICH_HOAT)) {
            ghi(LoaiThongBao.YEU_CAU_DOI_LICH_MOI, idTaiKhoan, null, yeuCau.getId(), noiDung);
        }
    }

    @Override
    public void ketQuaYeuCau(YeuCauDoiLich yeuCau) {
        boolean duyet = yeuCau.getTrangThai() == TrangThaiYeuCau.DA_DUYET;
        String ghiChu = yeuCau.getGhiChuXuLy();
        String noiDung = "Yêu cầu " + (yeuCau.getLoaiYeuCau() == LoaiYeuCauDoiLich.XIN_NGHI ? "xin nghỉ" : "đổi")
                + " ca " + moTaCa(yeuCau.getLichLamViec()) + (duyet ? " đã được duyệt." : " bị từ chối.")
                + (ghiChu == null ? "" : " Ghi chú: " + ghiChu);
        ghi(LoaiThongBao.KET_QUA_DUYET_DOI_LICH, yeuCau.getBacSi().getTaiKhoan().getId(), null, yeuCau.getId(),
                noiDung);
    }

    @Override
    public void caThayDoi(BacSi bacSi, String noiDung) {
        ghi(LoaiThongBao.CA_LAM_VIEC_THAY_DOI, bacSi.getTaiKhoan().getId(), null, null, noiDung);
    }

    @Override
    public void lichHenCanDoi(LichHen lichHen, String lyDo) {
        String noiDung = "Ca khám của lịch hẹn " + lichHen.getMaTraCuu() + " ("
                + GIO_NGAY.format(lichHen.getKhungGio().getGioBatDau()) + ") đã bị hủy. Lý do: " + lyDo
                + " Vui lòng đổi sang khung giờ khác hoặc hủy lịch hẹn.";
        for (Long idTaiKhoan : ThongBaoLichHenServiceImpl.taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_CAN_DOI, idTaiKhoan, lichHen.getId(), null, noiDung);
        }
    }

    @Override
    public void lichHenDoiPhong(LichHen lichHen) {
        PhongKham phong = lichHen.getPhongKham();
        String noiDung = "Lịch hẹn " + lichHen.getMaTraCuu() + " ("
                + GIO_NGAY.format(lichHen.getKhungGio().getGioBatDau()) + ") đổi sang phòng " + phong.getTenPhong()
                + (phong.getTang() == null ? "" : " (" + phong.getTang() + ")") + ". Giờ khám không đổi.";
        for (Long idTaiKhoan : ThongBaoLichHenServiceImpl.taiKhoanPhiaBenhNhan(lichHen)) {
            ghi(LoaiThongBao.LICH_HEN_DOI_PHONG, idTaiKhoan, lichHen.getId(), null, noiDung);
        }
    }

    /** "08:00-11:30 ngày 12/10/2026". */
    static String moTaCa(LichLamViec ca) {
        return GIO.format(ca.getGioBatDau()) + "-" + GIO.format(ca.getGioKetThuc()) + " ngày "
                + NGAY.format(ca.getNgayLamViec());
    }

    private void ghi(LoaiThongBao loai, Long idTaiKhoan, Long idLichHen, Long idYeuCau, String noiDung) {
        SuKienThongBao suKien = new SuKienThongBao();
        suKien.setLoai(loai);
        suKien.setIdTaiKhoan(idTaiKhoan);
        suKien.setIdLichHen(idLichHen);
        suKien.setIdYeuCau(idYeuCau);
        suKien.setNoiDung(noiDung);
        suKienThongBaoRepository.save(suKien);
    }

}
