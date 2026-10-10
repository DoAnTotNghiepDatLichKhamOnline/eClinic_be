package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.response.TongQuanBacSiResponse;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanDanhGiaResponse;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanQuanTriResponse;
import iuh.fit.se.eclinic.booking.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.TaiKhoanChiDocRepository;
import iuh.fit.se.eclinic.booking.service.DanhGiaService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.service.TongQuanService;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TongQuanServiceImpl implements TongQuanService {

    /** Lịch hẹn không còn là 1 lượt khám của ngày: đã hủy, đã hủy do đổi lịch, bị bác sĩ từ chối. */
    private static final List<TrangThaiLichHen> KHONG_TINH = List.of(TrangThaiLichHen.DA_HUY,
            TrangThaiLichHen.DA_HUY_DO_DOI_LICH, TrangThaiLichHen.BI_TU_CHOI);
    private static final List<TrangThaiLichHen> CHUA_KHAM = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final LichHenRepository lichHenRepository;
    private final TaiKhoanChiDocRepository taiKhoanChiDocRepository;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final TaiKhoanService taiKhoanService;
    private final DanhGiaService danhGiaService;

    @Override
    public TongQuanBacSiResponse cuaBacSi(Long idTaiKhoanBacSi) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        LocalDateTime bayGio = LocalDateTime.now();
        LocalDateTime dauNgay = bayGio.toLocalDate().atStartOfDay();
        LocalDateTime cuoiNgay = dauNgay.plusDays(1);
        TongQuanDanhGiaResponse danhGia = danhGiaService.tongQuanCuaBacSi(idTaiKhoanBacSi);
        return new TongQuanBacSiResponse(
                lichHenRepository.demCuaBacSiTrongKhoangKhongThuoc(idBacSi, dauNgay, cuoiNgay, KHONG_TINH),
                lichHenRepository.demChuaKhamCuaBacSiTrongKhoang(idBacSi, dauNgay, cuoiNgay, CHUA_KHAM),
                lichHenRepository.demCuaBacSiTrongKhoangThuoc(idBacSi, dauNgay, cuoiNgay,
                        List.of(TrangThaiLichHen.DA_HOAN_THANH)),
                danhGia.diemTrungBinh(), danhGia.soDanhGia(),
                lichHenRepository.demYeuCauChoXacNhan(idBacSi, TrangThaiLichHen.CHO_XAC_NHAN, bayGio), bayGio);
    }

    @Override
    public TongQuanQuanTriResponse cuaQuanTri() {
        LocalDateTime bayGio = LocalDateTime.now();
        LocalDate ngayDauThang = bayGio.toLocalDate().withDayOfMonth(1);
        return new TongQuanQuanTriResponse(bacSiChiDocRepository.demDangCongTacCoTaiKhoanHoatDong(),
                taiKhoanChiDocRepository.countByVaiTroAndTrangThai(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT),
                lichHenRepository.demDaTaoTu(ngayDauThang.atStartOfDay(), TrangThaiLichHen.DA_HUY_DO_DOI_LICH),
                bayGio);
    }
}
