package iuh.fit.se.eclinic.booking.service.impl;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.PhamViLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LanKhamCuaToiResponse;
import iuh.fit.se.eclinic.booking.mapper.KetQuaKhamMapper;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhAnChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.HoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.service.LichSuKhamService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichSuKhamServiceImpl implements LichSuKhamService {

    private final TaiKhoanService taiKhoanService;
    private final HoSoBenhNhanService hoSoBenhNhanService;
    private final LichHenRepository lichHenRepository;
    private final HoSoBenhAnChiDocRepository hoSoBenhAnChiDocRepository;
    private final LichHenMapper lichHenMapper;
    private final KetQuaKhamMapper ketQuaKhamMapper;

    @Override
    public TrangDuLieu<LanKhamCuaToiResponse> cuaToi(Long idTaiKhoan, PhamViLichHen phamVi, int trang,
            int kichThuoc) {
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        Long idHoSoCuaToi = hoSoBenhNhanService.timDaLienKetCuaTaiKhoan(idTaiKhoan).map(HoSoBenhNhan::getId)
                .orElse(null);
        Page<LichHen> daKham = lichHenRepository.timDaKhamDuocXemKetQua(idTaiKhoan, idHoSoCuaToi,
                TrangThaiLichHen.DA_HOAN_THANH, phamVi == PhamViLichHen.BAN_THAN, phamVi == PhamViLichHen.NGUOI_KHAC,
                PageRequest.of(trang, kichThuoc));
        // Bệnh án của cả trang trong 1 câu query
        Map<Long, HoSoBenhAn> benhAnTheoLichHen = daKham.isEmpty() ? Map.of()
                : hoSoBenhAnChiDocRepository
                        .timTheoLichHenKemDonThuoc(daKham.getContent().stream().map(LichHen::getId).toList()).stream()
                        .collect(Collectors.toMap(benhAn -> benhAn.getLichHen().getId(), Function.identity()));
        return TrangDuLieu.tu(daKham.map(lichHen -> new LanKhamCuaToiResponse(
                lichHenMapper.toLichHenCuaToi(lichHen, idHoSoCuaToi, idTaiKhoan),
                ketQuaKhamMapper.toResponse(benhAnTheoLichHen.get(lichHen.getId())))));
    }

}
