package iuh.fit.se.eclinic.booking.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.LocLichHen;
import iuh.fit.se.eclinic.booking.dto.request.PhamViLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LanKhamCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.TrangCaNhanResponse;
import iuh.fit.se.eclinic.booking.mapper.HoSoBenhNhanMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.booking.service.LichSuKhamService;
import iuh.fit.se.eclinic.booking.service.NguoiThanDaLuuService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.service.TrangCaNhanService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrangCaNhanServiceImpl implements TrangCaNhanService {

    private final TaiKhoanService taiKhoanService;
    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final HoSoBenhNhanMapper hoSoBenhNhanMapper;
    private final NguoiThanDaLuuService nguoiThanDaLuuService;
    private final LichHenService lichHenService;
    private final LichSuKhamService lichSuKhamService;

    @Override
    public TrangCaNhanResponse cuaToi(Long idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        TrangDuLieu<LichHenCuaToiResponse> cuaToi = lichHenService.lichHenCuaToi(idTaiKhoan, LocLichHen.SAP_TOI,
                PhamViLichHen.BAN_THAN, 0, SO_LICH_SAP_TOI);
        TrangDuLieu<LichHenCuaToiResponse> cuaNguoiKhac = lichHenService.lichHenCuaToi(idTaiKhoan, LocLichHen.SAP_TOI,
                PhamViLichHen.NGUOI_KHAC, 0, SO_LICH_SAP_TOI);
        // Chỉ cần tổng số: lấy trang 1 dòng
        long lichSu = lichHenService.lichHenCuaToi(idTaiKhoan, LocLichHen.LICH_SU, PhamViLichHen.TAT_CA, 0, 1)
                .tongSoPhanTu();
        TrangDuLieu<LanKhamCuaToiResponse> daKham = lichSuKhamService.cuaToi(idTaiKhoan, PhamViLichHen.TAT_CA, 0,
                SO_LAN_KHAM_GAN_DAY);
        return new TrangCaNhanResponse(
                hoSoBenhNhanRepository.findByTaiKhoanId(idTaiKhoan).map(hoSoBenhNhanMapper::toHoSoCuaToi).orElse(null),
                taiKhoan.getEmail(), nguoiThanDaLuuService.cuaTaiKhoan(idTaiKhoan),
                new TrangCaNhanResponse.LichSapToi(cuaToi.noiDung(), cuaNguoiKhac.noiDung()), daKham.noiDung(),
                new TrangCaNhanResponse.SoLich(cuaToi.tongSoPhanTu(), cuaNguoiKhac.tongSoPhanTu(), lichSu,
                        daKham.tongSoPhanTu()));
    }

}
