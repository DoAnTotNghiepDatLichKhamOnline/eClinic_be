package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoKhamResponse;
import iuh.fit.se.eclinic.booking.mapper.CaKhamMapper;
import iuh.fit.se.eclinic.booking.mapper.KetQuaKhamMapper;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhAnChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.HoSoKhamService;
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.booking.service.SuaHoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoSoKhamServiceImpl implements HoSoKhamService {

    private final TaiKhoanService taiKhoanService;
    private final LichHenService lichHenService;
    private final LichHenRepository lichHenRepository;
    private final HoSoBenhAnChiDocRepository hoSoBenhAnChiDocRepository;
    private final SuaHoSoBenhNhanService suaHoSoBenhNhanService;
    private final LichHenMapper lichHenMapper;
    private final CaKhamMapper caKhamMapper;
    private final KetQuaKhamMapper ketQuaKhamMapper;

    @Override
    public HoSoKhamResponse theoLichHen(Long idTaiKhoanBacSi, Long idLichHen) {
        return toResponse(layCuaBacSi(idTaiKhoanBacSi, lichHenRepository.timTheoIdKemChiTiet(idLichHen)));
    }

    @Override
    public HoSoKhamResponse theoMa(Long idTaiKhoanBacSi, String ma) {
        return toResponse(layCuaBacSi(idTaiKhoanBacSi, lichHenService.timTheoMa(ma)));
    }

    @Override
    @Transactional
    public HoSoKhamResponse suaBenhNhan(Long idTaiKhoanBacSi, Long idLichHen, SuaHoSoBenhNhanRequest request) {
        LichHen lichHen = layCuaBacSi(idTaiKhoanBacSi, lichHenRepository.timTheoIdKemChiTiet(idLichHen));
        // Cùng transaction: hồ sơ của lichHen chính là dòng vừa được sửa
        suaHoSoBenhNhanService.sua(lichHen.getHoSoBenhNhan().getId(), request, idTaiKhoanBacSi);
        return toResponse(lichHen);
    }

    @Override
    @Transactional
    public void daDoiChieu(Long idTaiKhoanBacSi, Long idLichHen) {
        layCuaBacSi(idTaiKhoanBacSi, lichHenRepository.timTheoIdKemChiTiet(idLichHen)).setCanDoiChieu(false);
    }

    /** Lịch hẹn không có và lịch hẹn của bác sĩ khác trả cùng 1 lỗi: không lộ lịch hẹn nào tồn tại. */
    private LichHen layCuaBacSi(Long idTaiKhoanBacSi, Optional<LichHen> lichHen) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        return lichHen
                .filter(timThay -> timThay.getBacSi().getId().equals(idBacSi))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy lịch hẹn"));
    }

    private HoSoKhamResponse toResponse(LichHen lichHen) {
        HoSoBenhNhan hoSo = lichHen.getHoSoBenhNhan();
        LocalDateTime gioKham = lichHen.getKhungGio().getGioBatDau();
        List<LichHen> truoc = lichHenRepository.timCuaHoSoTruoc(hoSo.getId(), gioKham);
        // Bệnh án của lịch hẹn đang mở và của các lần khám trước, trong 1 câu query
        List<Long> cacLichHen = Stream.concat(Stream.of(lichHen), truoc.stream()).map(LichHen::getId).toList();
        Map<Long, HoSoBenhAn> benhAnTheoLichHen = hoSoBenhAnChiDocRepository.timTheoLichHenKemDonThuoc(cacLichHen)
                .stream()
                .collect(Collectors.toMap(benhAn -> benhAn.getLichHen().getId(), Function.identity()));
        LocalDate ngaySinh = hoSo.getNgaySinh();
        Integer tuoi = ngaySinh == null ? null : Period.between(ngaySinh, gioKham.toLocalDate()).getYears();
        return new HoSoKhamResponse(lichHenMapper.toTrongCaResponse(lichHen, false),
                new HoSoKhamResponse.HoSoBenhNhan(hoSo.getId(), hoSo.getCccd(), hoSo.getHoTen(), ngaySinh, tuoi,
                        hoSo.getGioiTinh(), hoSo.getSoDienThoai(), hoSo.getDiaChi(), hoSo.getSoBaoHiemYTe(),
                        hoSo.getTienSuBenhLy(), hoSo.getTaiKhoan() != null),
                ketQuaKhamMapper.toResponse(benhAnTheoLichHen.get(lichHen.getId())),
                truoc.stream().map(lan -> toLanKham(lan, benhAnTheoLichHen.get(lan.getId()))).toList());
    }

    private HoSoKhamResponse.LanKham toLanKham(LichHen lichHen, HoSoBenhAn benhAn) {
        LocalDateTime gioKham = lichHen.getKhungGio().getGioBatDau();
        return new HoSoKhamResponse.LanKham(lichHen.getId(), lichHen.getMaTraCuu(), gioKham.toLocalDate(), gioKham,
                caKhamMapper.toBacSiTomTat(lichHen.getBacSi()), lichHen.getBacSi().getChuyenKhoa().getTenChuyenKhoa(),
                lichHen.getTrangThai(), lichHen.getLyDoKham(), ketQuaKhamMapper.toResponse(benhAn));
    }

}
