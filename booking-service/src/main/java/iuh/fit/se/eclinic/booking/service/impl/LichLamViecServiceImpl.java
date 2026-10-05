package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.mapper.LichLamViecMapper;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository.SoLuotCuaCa;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichLamViecServiceImpl implements LichLamViecService {

    /** 4 chữ số (hoặc 6 khi ngày quá đông); lịch hẹn có trước khi có mã dùng id nên có thể dài hơn. */
    private static final Pattern DANG_MA_TRA_CUU = Pattern.compile("(?i)ECL-\\d{8}-\\d{4,7}");
    /** Dạng của TokenNgauNhien.tao(), như PhieuKhamServiceImpl. */
    private static final Pattern DANG_MA_PHIEU_KHAM = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final LichLamViecRepository lichLamViecRepository;
    private final LichHenRepository lichHenRepository;
    private final TaiKhoanService taiKhoanService;
    private final LichLamViecMapper lichLamViecMapper;
    private final LichHenMapper lichHenMapper;

    @Override
    public LichLamViec layTheoId(Long id) {
        return lichLamViecRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("LichLamViec", id));
    }

    @Override
    public List<LichLamViec> timTheoBacSi(Long bacSiId, LocalDate tuNgay, LocalDate denNgay) {
        return lichLamViecRepository.findByBacSiIdAndNgayLamViecBetweenOrderByNgayLamViecAscGioBatDauAsc(bacSiId,
                tuNgay, denNgay);
    }

    @Override
    public void kiemTraKhongTrungLich(Long bacSiId, Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeLichLamViecId) {
        if (!gioKetThuc.isAfter(gioBatDau)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (lichLamViecRepository.existsTrungCaCuaBacSi(bacSiId, ngayLamViec, gioBatDau, gioKetThuc,
                excludeLichLamViecId)) {
            throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC, "Bác sĩ đã có ca làm việc trùng giờ");
        }
        if (lichLamViecRepository.existsTrungCaCuaPhongKham(phongKhamId, ngayLamViec, gioBatDau, gioKetThuc,
                excludeLichLamViecId)) {
            throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC, "Phòng khám đã được xếp ca trùng giờ");
        }
    }

    @Override
    public List<CaLamViecResponse> lichCuaBacSi(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay) {
        kiemTraKhoangNgay(tuNgay, denNgay);
        // Bác sĩ lấy từ tài khoản đang đăng nhập, không nhận id bác sĩ từ request
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoan);
        return kemSoLuot(lichLamViecRepository.timTrongKhoang(tuNgay, denNgay, null, bacSi.getId(), null));
    }

    @Override
    public List<CaLamViecResponse> lichToanVien(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi,
            Long idPhongKham) {
        kiemTraKhoangNgay(tuNgay, denNgay);
        return kemSoLuot(lichLamViecRepository.timTrongKhoang(tuNgay, denNgay, idChuyenKhoa, idBacSi, idPhongKham));
    }

    @Override
    public List<LichHenTrongCaResponse> lichHenCuaBacSiTheoNgay(Long idTaiKhoan, LocalDate ngay) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoan);
        return lichHenRepository
                .timCuaBacSiTrongKhoang(bacSi.getId(), ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay()).stream()
                .map(lichHen -> lichHenMapper.toTrongCaResponse(lichHen, false))
                .toList();
    }

    @Override
    public List<LichHenTrongCaResponse> lichHenCuaCa(Long idLichLamViec) {
        if (!lichLamViecRepository.existsById(idLichLamViec)) {
            throw new LoiKhongTimThay("LichLamViec", idLichLamViec);
        }
        return lichHenRepository.timTheoCa(idLichLamViec).stream()
                .map(lichHen -> lichHenMapper.toTrongCaResponse(lichHen, true))
                .toList();
    }

    @Override
    public LichHenTrongCaResponse traCuuLichHen(Long idTaiKhoanBacSi, String ma) {
        Long idBacSi = idTaiKhoanBacSi == null ? null : taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        String maGon = ma == null ? "" : ma.trim();
        Optional<LichHen> lichHen;
        if (DANG_MA_TRA_CUU.matcher(maGon).matches()) {
            lichHen = lichHenRepository.timTheoMaTraCuu(maGon.toUpperCase(Locale.ROOT));
        } else if (DANG_MA_PHIEU_KHAM.matcher(maGon).matches()) {
            // Collation của cột không phân biệt hoa thường; mã phiếu khám phải khớp từng ký tự
            lichHen = lichHenRepository.timTheoMaPhieuKham(maGon)
                    .filter(timThay -> timThay.getMaTokenPhieuKham().equals(maGon));
        } else {
            lichHen = Optional.empty();
        }
        return lichHen
                .filter(timThay -> idBacSi == null || timThay.getBacSi().getId().equals(idBacSi))
                .map(timThay -> lichHenMapper.toTrongCaResponse(timThay, idBacSi == null))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy lịch hẹn với mã này"));
    }

    /** Gắn số lượt khám cho từng ca bằng 1 câu query đếm cho mọi ca. */
    private List<CaLamViecResponse> kemSoLuot(List<LichLamViec> cacCa) {
        if (cacCa.isEmpty()) {
            return List.of();
        }
        Map<Long, SoLuotCuaCa> soLuotTheoCa = lichLamViecRepository
                .demLuotTheoCa(cacCa.stream().map(LichLamViec::getId).toList()).stream()
                .collect(Collectors.toMap(SoLuotCuaCa::getIdLichLamViec, Function.identity()));
        return cacCa.stream().map(ca -> lichLamViecMapper.toResponse(ca, soLuotTheoCa.get(ca.getId()))).toList();
    }

    private static void kiemTraKhoangNgay(LocalDate tuNgay, LocalDate denNgay) {
        if (denNgay.isBefore(tuNgay)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "denNgay phải từ tuNgay trở đi");
        }
        if (ChronoUnit.DAYS.between(tuNgay, denNgay) >= SO_NGAY_XEM_TOI_DA) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "Mỗi lần chỉ xem được tối đa " + SO_NGAY_XEM_TOI_DA + " ngày");
        }
    }

}
