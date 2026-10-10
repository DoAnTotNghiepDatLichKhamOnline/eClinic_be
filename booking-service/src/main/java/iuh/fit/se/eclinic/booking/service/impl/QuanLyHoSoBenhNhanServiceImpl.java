package iuh.fit.se.eclinic.booking.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanDongResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository.SoLichHenTheoHoSo;
import iuh.fit.se.eclinic.booking.service.QuanLyHoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.util.CheThongTin;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.util.MauTimKiem;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuanLyHoSoBenhNhanServiceImpl implements QuanLyHoSoBenhNhanService {

    /** Số CCCD chỉ được tra khi gõ đủ 12 số: gõ 1 phần số CCCD không dò ra được hồ sơ nào. */
    private static final String DANG_CCCD = "\\d{12}";

    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final LichHenRepository lichHenRepository;
    private final LichHenMapper lichHenMapper;

    @Override
    public TrangDuLieu<HoSoBenhNhanDongResponse> danhSach(String tuKhoa, TrangThaiLienKet trangThaiLienKet, int trang,
            int kichThuoc) {
        String daCat = tuKhoa == null ? "" : tuKhoa.trim();
        Page<HoSoBenhNhan> ketQua = hoSoBenhNhanRepository.timChoQuanTri(MauTimKiem.chua(daCat),
                daCat.matches(DANG_CCCD) ? daCat : null, trangThaiLienKet, PageRequest.of(trang, kichThuoc));
        List<Long> idCacHoSo = ketQua.getContent().stream().map(HoSoBenhNhan::getId).toList();
        Map<Long, Long> soLichHen = idCacHoSo.isEmpty() ? Map.of()
                : lichHenRepository.demTheoHoSo(idCacHoSo).stream()
                        .collect(Collectors.toMap(SoLichHenTheoHoSo::getIdHoSo, SoLichHenTheoHoSo::getSoLichHen));
        return TrangDuLieu.tu(ketQua.map(hoSo -> new HoSoBenhNhanDongResponse(hoSo.getId(), hoSo.getHoTen(),
                hoSo.getNgaySinh(), hoSo.getGioiTinh(), hoSo.getSoDienThoai(),
                CheThongTin.cccdGiuBonSoCuoi(hoSo.getCccd()), hoSo.getTaiKhoan() != null, hoSo.getTrangThaiLienKet(),
                soLichHen.getOrDefault(hoSo.getId(), 0L), hoSo.getNgayTao())));
    }

    @Override
    public TrangDuLieu<LichHenTrongCaResponse> lichHen(Long idHoSoBenhNhan, int trang, int kichThuoc) {
        if (!hoSoBenhNhanRepository.existsById(idHoSoBenhNhan)) {
            throw new LoiKhongTimThay("HoSoBenhNhan", idHoSoBenhNhan);
        }
        return TrangDuLieu.tu(lichHenRepository.timCuaHoSo(idHoSoBenhNhan, PageRequest.of(trang, kichThuoc))
                .map(lichHen -> lichHenMapper.toTrongCaResponse(lichHen, true)));
    }
}
