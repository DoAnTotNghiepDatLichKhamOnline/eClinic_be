package iuh.fit.se.eclinic.medical.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.medical.ChiTietDonThuoc;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.TokenNgauNhien;
import iuh.fit.se.eclinic.medical.dto.request.BenhAnRequest;
import iuh.fit.se.eclinic.medical.dto.request.DonThuocRequest;
import iuh.fit.se.eclinic.medical.dto.response.BenhAnResponse;
import iuh.fit.se.eclinic.medical.mapper.BenhAnMapper;
import iuh.fit.se.eclinic.medical.repository.HoSoBenhAnRepository;
import iuh.fit.se.eclinic.medical.repository.LichHenKhamRepository;
import iuh.fit.se.eclinic.medical.service.BacSiDangNhapService;
import iuh.fit.se.eclinic.medical.service.KhamBenhService;
import iuh.fit.se.eclinic.medical.service.ThuocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KhamBenhServiceImpl implements KhamBenhService {

    /** Lịch hẹn còn chờ khám. Lịch bác sĩ chưa kịp xác nhận (CHO_XAC_NHAN) vẫn khám được vào ngày khám. */
    private static final List<TrangThaiLichHen> TRANG_THAI_KHAM_DUOC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final BacSiDangNhapService bacSiDangNhapService;
    private final LichHenKhamRepository lichHenKhamRepository;
    private final HoSoBenhAnRepository hoSoBenhAnRepository;
    private final ThuocService thuocService;
    private final BenhAnMapper benhAnMapper;

    @Override
    @Transactional
    public BenhAnResponse ghiNhan(Long idTaiKhoanBacSi, Long idLichHen, BenhAnRequest request) {
        BacSi bacSi = bacSiDangNhapService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        // Khoá dòng lịch hẹn rồi mới kiểm tra: bấm lưu 2 lần thì lần sau thấy lịch hẹn đã hoàn thành
        LichHen lichHen = cuaBacSi(bacSi, lichHenKhamRepository.findByIdForUpdate(idLichHen));
        if (!TRANG_THAI_KHAM_DUOC.contains(lichHen.getTrangThai())) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_KHONG_KHAM_DUOC);
        }
        if (lichHen.getKhungGio().getGioBatDau().toLocalDate().isAfter(LocalDate.now())) {
            throw new LoiNghiepVu(MaLoi.CHUA_DEN_NGAY_KHAM);
        }
        HoSoBenhAn benhAn = new HoSoBenhAn();
        benhAn.setLichHen(lichHen);
        benhAn.setHoSoBenhNhan(lichHen.getHoSoBenhNhan());
        benhAn.setBacSi(bacSi);
        benhAn.setMaTokenSoKham(TokenNgauNhien.tao());
        ganNoiDung(benhAn, request, bacSi.getId());
        hoSoBenhAnRepository.saveAndFlush(benhAn);
        // Ngoại lệ có chủ đích (xem LichHenKhamRepository): chỉ đổi trạng thái, cùng transaction với hồ sơ bệnh án
        lichHen.setTrangThai(TrangThaiLichHen.DA_HOAN_THANH);
        log.info("Bác sĩ id={} ghi nhận kết quả khám cho lịch hẹn id={}", bacSi.getId(), idLichHen);
        return benhAnMapper.toResponse(benhAn, lichHen);
    }

    @Override
    @Transactional
    public BenhAnResponse sua(Long idTaiKhoanBacSi, Long idLichHen, BenhAnRequest request) {
        BacSi bacSi = bacSiDangNhapService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        LichHen lichHen = cuaBacSi(bacSi, lichHenKhamRepository.findById(idLichHen));
        HoSoBenhAn benhAn = layBenhAn(idLichHen);
        ganNoiDung(benhAn, request, bacSi.getId());
        hoSoBenhAnRepository.saveAndFlush(benhAn);
        log.info("Bác sĩ id={} sửa hồ sơ bệnh án của lịch hẹn id={}", bacSi.getId(), idLichHen);
        return benhAnMapper.toResponse(benhAn, lichHen);
    }

    @Override
    public BenhAnResponse xem(Long idTaiKhoanBacSi, Long idLichHen) {
        BacSi bacSi = bacSiDangNhapService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        LichHen lichHen = cuaBacSi(bacSi, lichHenKhamRepository.findById(idLichHen));
        return benhAnMapper.toResponse(layBenhAn(idLichHen), lichHen);
    }

    /** Lịch hẹn không có và lịch hẹn của bác sĩ khác trả cùng 1 lỗi: không lộ lịch hẹn nào tồn tại. */
    private static LichHen cuaBacSi(BacSi bacSi, Optional<LichHen> lichHen) {
        return lichHen
                .filter(timThay -> timThay.getBacSi().getId().equals(bacSi.getId()))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy lịch hẹn"));
    }

    private HoSoBenhAn layBenhAn(Long idLichHen) {
        return hoSoBenhAnRepository.timTheoLichHenKemDonThuoc(idLichHen)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Lịch hẹn này chưa có hồ sơ bệnh án"));
    }

    /** Ghi chẩn đoán, ghi chú, ngày tái khám và thay toàn bộ đơn thuốc bằng các dòng trong request. */
    private void ganNoiDung(HoSoBenhAn benhAn, BenhAnRequest request, Long idBacSi) {
        // Tra / thêm thuốc trước khi đụng tới hồ sơ bệnh án: việc thêm thuốc chạy câu lệnh native
        List<DonThuocRequest> cacDong = request.donThuoc() == null ? List.of() : request.donThuoc();
        // Thuốc đơn này đã có từ trước vẫn giữ được dù quản trị viên đã cho ngừng dùng; chỉ dòng thêm mới bị từ chối
        Set<Long> idThuocDaCo = benhAn.getChiTietDonThuoc().stream()
                .map(chiTiet -> chiTiet.getThuoc().getId())
                .collect(Collectors.toSet());
        List<Thuoc> cacThuoc = new ArrayList<>();
        for (DonThuocRequest dong : cacDong) {
            cacThuoc.add(thuocService.layHoacTao(dong.tenThuoc(), rongThanhNull(dong.donVi()), idBacSi, idThuocDaCo));
        }
        benhAn.setChanDoan(request.chanDoan().trim());
        benhAn.setGhiChu(rongThanhNull(request.ghiChu()));
        benhAn.setNgayTaiKhamDeXuat(request.ngayTaiKhamDeXuat());
        new ArrayList<>(benhAn.getChiTietDonThuoc()).forEach(benhAn::removeChiTietDonThuoc);
        for (int i = 0; i < cacDong.size(); i++) {
            DonThuocRequest dong = cacDong.get(i);
            ChiTietDonThuoc chiTiet = new ChiTietDonThuoc();
            chiTiet.setThuoc(cacThuoc.get(i));
            chiTiet.setLieuDung(rongThanhNull(dong.lieuDung()));
            chiTiet.setSoLanMoiNgay(dong.soLanMoiNgay());
            chiTiet.setSoNgayDung(dong.soNgayDung());
            chiTiet.setGhiChuSuDung(rongThanhNull(dong.ghiChuSuDung()));
            benhAn.addChiTietDonThuoc(chiTiet);
        }
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }

}
