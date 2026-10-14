package iuh.fit.se.eclinic.medical.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.config.BenhAnProperties;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.medical.ChiTietDonThuoc;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TruongBenhAn;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.TokenNgauNhien;
import iuh.fit.se.eclinic.medical.dto.request.BenhAnRequest;
import iuh.fit.se.eclinic.medical.dto.request.DonThuocRequest;
import iuh.fit.se.eclinic.medical.dto.response.BenhAnResponse;
import iuh.fit.se.eclinic.medical.dto.response.NoiDungBenhAn;
import iuh.fit.se.eclinic.medical.mapper.BenhAnMapper;
import iuh.fit.se.eclinic.medical.repository.HoSoBenhAnRepository;
import iuh.fit.se.eclinic.medical.repository.LichHenKhamRepository;
import iuh.fit.se.eclinic.medical.service.BacSiDangNhapService;
import iuh.fit.se.eclinic.medical.service.KhamBenhService;
import iuh.fit.se.eclinic.medical.service.LichSuSuaBenhAnService;
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
    private final LichSuSuaBenhAnService lichSuSuaBenhAnService;
    private final BenhAnMapper benhAnMapper;
    private final BenhAnProperties benhAnProperties;

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
        ganNoiDung(benhAn, request);
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
        // Khoá dòng lịch hẹn: 2 lần sửa cùng lúc chạy lần lượt, lần sau thấy phiên bản đã tăng
        LichHen lichHen = cuaBacSi(bacSi, lichHenKhamRepository.findByIdForUpdate(idLichHen));
        HoSoBenhAn benhAn = layBenhAn(idLichHen);
        if (LocalDateTime.now().isAfter(benhAnProperties.hanSua(benhAn.getNgayTao()))) {
            throw new LoiNghiepVu(MaLoi.HET_HAN_SUA_BENH_AN);
        }
        if (request.phienBan() != benhAn.getPhienBan()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_DA_THAY_DOI);
        }
        NoiDungBenhAn truoc = NoiDungBenhAn.tu(benhAn);
        ganNoiDung(benhAn, request);
        NoiDungBenhAn sau = NoiDungBenhAn.tu(benhAn);
        List<TruongBenhAn> cacTruongDoi = NoiDungBenhAn.cacTruongDoi(truoc, sau);
        if (cacTruongDoi.isEmpty()) {
            // Ném trong transaction: đơn thuốc vừa thay trong bộ nhớ bị rollback, không ghi gì
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.benhAnKhongDoi");
        }
        benhAn.setPhienBan(benhAn.getPhienBan() + 1);
        benhAn.setNgaySuaCuoi(LocalDateTime.now());
        hoSoBenhAnRepository.saveAndFlush(benhAn);
        lichSuSuaBenhAnService.ghi(benhAn, bacSi, request.lyDoSua().trim(), truoc, sau);
        log.info("Bác sĩ id={} sửa hồ sơ bệnh án của lịch hẹn id={} lên phiên bản {}, đổi {}", bacSi.getId(), idLichHen,
                benhAn.getPhienBan(), cacTruongDoi);
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
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.lichHen"));
    }

    private HoSoBenhAn layBenhAn(Long idLichHen) {
        return hoSoBenhAnRepository.timTheoLichHenKemDonThuoc(idLichHen)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.benhAn"));
    }

    /** Ghi chẩn đoán, ghi chú, ngày tái khám và thay toàn bộ đơn thuốc bằng các dòng trong request. */
    private void ganNoiDung(HoSoBenhAn benhAn, BenhAnRequest request) {
        // Kiểm tra thuốc trước khi đụng tới hồ sơ bệnh án: đơn có thuốc không kê được thì không đổi gì
        List<DonThuocRequest> cacDong = request.donThuoc() == null ? List.of() : request.donThuoc();
        // Thuốc đơn này đã có từ trước vẫn giữ được dù đã ngừng dùng / chưa xác minh; chỉ dòng thêm mới bị từ chối
        Set<Long> idThuocDaCo = benhAn.getChiTietDonThuoc().stream()
                .map(chiTiet -> chiTiet.getThuoc().getId())
                .collect(Collectors.toSet());
        List<Long> idCacThuoc = cacDong.stream().map(DonThuocRequest::idThuoc).toList();
        Map<Long, Thuoc> thuocTheoId = thuocService.layDeKe(idCacThuoc, idThuocDaCo);
        Set<Long> daGap = new HashSet<>();
        for (Long idThuoc : idCacThuoc) {
            if (!daGap.add(idThuoc)) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.thuocTrungLap",
                        thuocTheoId.get(idThuoc).getTenThuoc());
            }
        }
        LocalDate ngayTaiKham = request.ngayTaiKhamDeXuat();
        // Sửa mà giữ nguyên ngày tái khám cũ thì được dù ngày đó đã qua; ngày mới phải sau hôm nay
        if (ngayTaiKham != null && !ngayTaiKham.isAfter(LocalDate.now())
                && !ngayTaiKham.equals(benhAn.getNgayTaiKhamDeXuat())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.ngayTaiKham");
        }
        benhAn.setChanDoan(request.chanDoan().trim());
        benhAn.setGhiChu(rongThanhNull(request.ghiChu()));
        benhAn.setNgayTaiKhamDeXuat(request.ngayTaiKhamDeXuat());
        new ArrayList<>(benhAn.getChiTietDonThuoc()).forEach(benhAn::removeChiTietDonThuoc);
        for (DonThuocRequest dong : cacDong) {
            ChiTietDonThuoc chiTiet = new ChiTietDonThuoc();
            chiTiet.setThuoc(thuocTheoId.get(dong.idThuoc()));
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
