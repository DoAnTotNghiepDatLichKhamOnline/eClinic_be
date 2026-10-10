package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.util.ChuanHoaTen;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
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

    private final LichLamViecRepository lichLamViecRepository;
    private final LichHenRepository lichHenRepository;
    private final TaiKhoanService taiKhoanService;
    private final LichLamViecMapper lichLamViecMapper;
    private final LichHenMapper lichHenMapper;
    private final LichHenService lichHenService;

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
        KhoangNgay.kiemTra(tuNgay, denNgay);
        // Bác sĩ lấy từ tài khoản đang đăng nhập, không nhận id bác sĩ từ request
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoan);
        return kemSoLuot(lichLamViecRepository.timTrongKhoang(tuNgay, denNgay, null, bacSi.getId(), null));
    }

    @Override
    public List<CaLamViecResponse> lichToanVien(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi,
            Long idPhongKham) {
        KhoangNgay.kiemTra(tuNgay, denNgay);
        return kemSoLuot(lichLamViecRepository.timTrongKhoang(tuNgay, denNgay, idChuyenKhoa, idBacSi, idPhongKham));
    }

    @Override
    public List<LichHenTrongCaResponse> lichHenCuaBacSiTheoNgay(Long idTaiKhoan, LocalDate ngay, Long idLichLamViec,
            Integer soThuTu, String tuKhoa) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoan);
        LocalDate ngayXem = ngay == null ? LocalDate.now() : ngay;
        String tuKhoaGon = tuKhoa == null || tuKhoa.isBlank() ? null : ChuanHoaTen.khongDau(tuKhoa);
        // Lịch hẹn 1 ngày của 1 bác sĩ chỉ vài chục dòng: lọc trên danh sách đã tải, so tên không dấu như lúc đặt lịch
        return lichHenRepository
                .timCuaBacSiTrongKhoang(bacSi.getId(), ngayXem.atStartOfDay(), ngayXem.plusDays(1).atStartOfDay())
                .stream()
                .filter(lichHen -> idLichLamViec == null
                        || lichHen.getKhungGio().getLichLamViec().getId().equals(idLichLamViec))
                .filter(lichHen -> soThuTu == null || soThuTu.equals(lichHen.getSoThuTu()))
                .filter(lichHen -> tuKhoaGon == null || coTen(lichHen, tuKhoaGon))
                .map(lichHen -> lichHenMapper.toTrongCaResponse(lichHen, false))
                .toList();
    }

    /** Họ tên trong hồ sơ hoặc họ tên người đặt đã nhập chứa từ khoá (đã bỏ dấu). */
    private static boolean coTen(LichHen lichHen, String tuKhoaKhongDau) {
        return ChuanHoaTen.khongDau(lichHen.getHoSoBenhNhan().getHoTen()).contains(tuKhoaKhongDau)
                || (lichHen.getHoTenDaNhap() != null
                        && ChuanHoaTen.khongDau(lichHen.getHoTenDaNhap()).contains(tuKhoaKhongDau));
    }

    @Override
    public List<LichHenTrongCaResponse> lichHenCuaBacSiTrongKhoang(Long idTaiKhoan, LocalDate tuNgay,
            LocalDate denNgay) {
        KhoangNgay.kiemTra(tuNgay, denNgay);
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoan);
        return lichHenRepository
                .timCuaBacSiTrongKhoang(bacSi.getId(), tuNgay.atStartOfDay(), denNgay.plusDays(1).atStartOfDay())
                .stream()
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
        return lichHenService.timTheoMa(ma)
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

}
