package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.KhungGioKhamRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.LuotKhamService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class LuotKhamServiceImpl implements LuotKhamService {

    private static final List<TrangThaiLichHen> CON_HIEU_LUC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final KhungGioKhamRepository khungGioKhamRepository;
    private final LichHenRepository lichHenRepository;

    @Override
    public void traLuot(LichHen lichHen) {
        KhungGioKham luot = khungGioKhamRepository.findByIdForUpdate(lichHen.getKhungGio().getId()).orElseThrow();
        if (luot.getTrangThai() == TrangThaiKhungGio.DA_DAT) {
            luot.setTrangThai(TrangThaiKhungGio.CON_TRONG);
        }
    }

    @Override
    public void danhLaiSoThuTu(Long idPhongKham, LocalDate ngay) {
        // Lượt khám mới thêm và phòng khám mới của lịch hẹn phải xuống DB trước khi đếm
        khungGioKhamRepository.flush();
        for (LichHen lichHen : lichHenRepository.timCuaPhongTrongNgay(idPhongKham, ngay, CON_HIEU_LUC)) {
            KhungGioKham luot = lichHen.getKhungGio();
            int soThuTu = (int) khungGioKhamRepository.demLuotDungTruoc(idPhongKham, ngay, luot.getGioBatDau(),
                    luot.getId()) + 1;
            if (!Integer.valueOf(soThuTu).equals(lichHen.getSoThuTu())) {
                lichHen.setSoThuTu(soThuTu);
            }
        }
    }

}
