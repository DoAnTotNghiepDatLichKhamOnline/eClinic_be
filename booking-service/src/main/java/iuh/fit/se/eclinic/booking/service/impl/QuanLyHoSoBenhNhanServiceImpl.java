package iuh.fit.se.eclinic.booking.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanDongResponse;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository.SoLichHenTheoHoSo;
import iuh.fit.se.eclinic.booking.repository.NguoiGiamHoRepository;
import iuh.fit.se.eclinic.booking.service.QuanLyHoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.util.CheThongTin;
import iuh.fit.se.eclinic.booking.util.ChuanHoaTen;
import iuh.fit.se.eclinic.booking.util.KhoaNhanDien;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.MauTimKiem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuanLyHoSoBenhNhanServiceImpl implements QuanLyHoSoBenhNhanService {

    /** Số CCCD chỉ được tra khi gõ đủ 12 số: gõ 1 phần số CCCD không dò ra được hồ sơ nào. */
    private static final String DANG_CCCD = "\\d{12}";

    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final LichHenRepository lichHenRepository;
    private final LichHenMapper lichHenMapper;
    private final NguoiGiamHoRepository nguoiGiamHoRepository;

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

    @Override
    public HoSoBenhNhanQuanTriResponse xem(Long id) {
        return toResponse(hoSoBenhNhanRepository.findById(id)
                .orElseThrow(() -> new LoiKhongTimThay("HoSoBenhNhan", id)));
    }

    @Override
    @Transactional
    public HoSoBenhNhanQuanTriResponse sua(Long id, SuaHoSoBenhNhanRequest request, Long idNguoiSua) {
        // Khoá dòng hồ sơ như lúc đặt lịch để lần đặt đang chạy không đọc dữ liệu sửa dở
        HoSoBenhNhan hoSo = hoSoBenhNhanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new LoiKhongTimThay("HoSoBenhNhan", id));
        String cccd = rongThanhNull(request.cccd());
        if (cccd != null && !cccd.equals(hoSo.getCccd())) {
            if (hoSo.getCccd() != null) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.doiCccd");
            }
            hoSo.setCccd(cccd);
        }
        hoSo.setHoTen(ChuanHoaTen.gon(request.hoTen()));
        hoSo.setNgaySinh(request.ngaySinh());
        hoSo.setGioiTinh(request.gioiTinh());
        hoSo.setSoDienThoai(request.soDienThoai());
        hoSo.setDiaChi(rongThanhNull(request.diaChi()));
        hoSo.setSoBaoHiemYTe(rongThanhNull(request.soBaoHiemYTe()));
        if (hoSo.getCccd() == null) {
            // Hồ sơ chưa có CCCD: tính lại khoá nhận diện theo thông tin mới để lần đặt sau vẫn về đúng hồ sơ
            nguoiGiamHoRepository.findFirstByHoSoBenhNhanIdAndNguoiGiamHoCccdNotNullOrderByIdAsc(id)
                    .ifPresent(lichHen -> hoSo.setKhoaNhanDien(KhoaNhanDien.tao(hoSo.getHoTen(), hoSo.getNgaySinh(),
                            lichHen.getNguoiGiamHo().getCccd())));
        }
        // Flush ngay: số CCCD / khoá nhận diện trùng hồ sơ khác vi phạm UNIQUE -> 409 XUNG_DOT_DU_LIEU
        hoSoBenhNhanRepository.saveAndFlush(hoSo);
        log.info("Tài khoản id={} sửa hồ sơ bệnh nhân id={}", idNguoiSua, id);
        return toResponse(hoSo);
    }

    private static HoSoBenhNhanQuanTriResponse toResponse(HoSoBenhNhan hoSo) {
        return new HoSoBenhNhanQuanTriResponse(hoSo.getId(), hoSo.getCccd(), hoSo.getHoTen(), hoSo.getNgaySinh(),
                hoSo.getGioiTinh(), hoSo.getSoDienThoai(), hoSo.getDiaChi(), hoSo.getSoBaoHiemYTe(),
                hoSo.getTrangThaiLienKet(), hoSo.getTaiKhoan() == null ? null : hoSo.getTaiKhoan().getId(),
                hoSo.getNgayTao());
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }

}
