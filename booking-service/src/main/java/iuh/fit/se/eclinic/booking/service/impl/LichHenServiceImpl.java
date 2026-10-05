package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.LocLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LichHenCuaToiResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichHenServiceImpl implements LichHenService {

    /** Lịch hẹn còn phải khám: cùng lượt khám chưa kết thúc thì là lịch "sắp tới". */
    private static final List<TrangThaiLichHen> TRANG_THAI_CON_HIEU_LUC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final LichHenRepository lichHenRepository;
    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final TaiKhoanService taiKhoanService;
    private final LichHenMapper lichHenMapper;

    @Override
    public LichHen layTheoId(Long id) {
        return lichHenRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("LichHen", id));
    }

    @Override
    public List<LichHen> timTheoHoSoBenhNhan(Long hoSoBenhNhanId) {
        return lichHenRepository.findByHoSoBenhNhanIdOrderByNgayTaoDesc(hoSoBenhNhanId);
    }

    @Override
    public List<LichHen> timTheoBacSiVaTrangThai(Long bacSiId, TrangThaiLichHen trangThai) {
        return lichHenRepository.findByBacSiIdAndTrangThaiOrderByNgayTaoDesc(bacSiId, trangThai);
    }

    @Override
    public TrangDuLieu<LichHenCuaToiResponse> lichHenCuaToi(Long idTaiKhoan, LocLichHen loc, int trang,
            int kichThuoc) {
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        Pageable phanTrang = PageRequest.of(trang, kichThuoc);
        LocalDateTime bayGio = LocalDateTime.now();
        // Hồ sơ chờ xác minh chưa được tính là của tài khoản: lịch hẹn của hồ sơ đó chưa hiện (quy tắc #3)
        HoSoBenhNhan hoSoCuaToi = hoSoBenhNhanRepository.findByTaiKhoanId(idTaiKhoan)
                .filter(hoSo -> hoSo.getTrangThaiLienKet() == TrangThaiLienKet.DA_LIEN_KET)
                .orElse(null);
        Long idHoSoCuaToi = hoSoCuaToi == null ? null : hoSoCuaToi.getId();
        String cccdCuaToi = hoSoCuaToi == null ? null : hoSoCuaToi.getCccd();
        Page<LichHen> lichHen = switch (loc) {
            case TAT_CA -> lichHenRepository.timCuaTaiKhoan(idTaiKhoan, idHoSoCuaToi, cccdCuaToi, phanTrang);
            case SAP_TOI -> lichHenRepository.timSapToiCuaTaiKhoan(idTaiKhoan, idHoSoCuaToi, cccdCuaToi,
                    TRANG_THAI_CON_HIEU_LUC, bayGio, phanTrang);
            case LICH_SU -> lichHenRepository.timLichSuCuaTaiKhoan(idTaiKhoan, idHoSoCuaToi, cccdCuaToi,
                    TRANG_THAI_CON_HIEU_LUC, bayGio, phanTrang);
        };
        return TrangDuLieu.tu(lichHen.map(l -> lichHenMapper.toLichHenCuaToi(l, idHoSoCuaToi)));
    }

}
