package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import iuh.fit.se.eclinic.booking.config.CacheConfig;
import iuh.fit.se.eclinic.booking.dto.request.KhaiBaoNgayNghiRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhaiBaoNgayNghiResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhaiBaoNgayNghiResponse.CaBiHuy;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiResponse;
import iuh.fit.se.eclinic.booking.mapper.NgayNghiMapper;
import iuh.fit.se.eclinic.booking.repository.CauHinhHeThongRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.NgayNghiBenhVienRepository;
import iuh.fit.se.eclinic.booking.service.CauHinhService;
import iuh.fit.se.eclinic.booking.service.LyDoHuyCa;
import iuh.fit.se.eclinic.booking.service.NgayNghiService;
import iuh.fit.se.eclinic.booking.service.NguonThayDoi;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichLamViecService;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.NgayNghiBenhVien;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NgayNghiServiceImpl implements NgayNghiService {

    /** Danh sách ngày nghỉ: tối đa 1 năm (366 ngày) mỗi lần xem. */
    private static final int SO_NGAY_XEM_TOI_DA = 366;

    private static final Comparator<LichLamViec> THEO_NGAY_GIO = Comparator.comparing(LichLamViec::getNgayLamViec)
            .thenComparing(LichLamViec::getGioBatDau);

    private final NgayNghiBenhVienRepository ngayNghiBenhVienRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final CauHinhHeThongRepository cauHinhHeThongRepository;
    private final CauHinhService cauHinhService;
    private final QuanLyCaService quanLyCaService;
    private final ThongBaoLichLamViecService thongBaoLichLamViecService;
    private final NgayNghiMapper ngayNghiMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NgayNghiQuanTriResponse> danhSach(LocalDate tuNgay, LocalDate denNgay) {
        return trongKhoang(tuNgay, denNgay).stream().map(ngayNghi -> ngayNghiMapper.toQuanTri(ngayNghi, true)).toList();
    }

    /** Khoá có ngày bắt đầu thật (mặc định hôm nay), nên sang ngày mới là sang khoá mới. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.NGAY_NGHI, key = "T(iuh.fit.se.eclinic.common.enums.NgonNgu).hienTai() + ':'"
            + " + (#tuNgay ?: T(java.time.LocalDate).now()) + ':' + #denNgay")
    public List<NgayNghiResponse> congKhai(LocalDate tuNgay, LocalDate denNgay) {
        return trongKhoang(tuNgay, denNgay).stream().map(ngayNghiMapper::toCongKhai).toList();
    }

    private List<NgayNghiBenhVien> trongKhoang(LocalDate tuNgay, LocalDate denNgay) {
        LocalDate tu = tuNgay == null ? LocalDate.now() : tuNgay;
        LocalDate den = denNgay == null ? tu.plusDays(SO_NGAY_XEM_TOI_DA - 1) : denNgay;
        KhoangNgay.kiemTra(tu, den, SO_NGAY_XEM_TOI_DA);
        return ngayNghiBenhVienRepository.timTrongKhoang(tu, den);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(cacheNames = CacheConfig.NGAY_NGHI, allEntries = true, condition = "#luu")
    public KetQuaKhaiBaoNgayNghiResponse khaiBao(Long idTaiKhoanQuanTri, KhaiBaoNgayNghiRequest request, boolean luu) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoanQuanTri);
        // Ca đã bắt đầu không hủy được; nghỉ đột xuất trong ngày thì quản trị viên hủy từng ca
        if (!request.tuNgay().isAfter(LocalDate.now())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.ngayNghiTuNgayMai");
        }
        KhoangNgay.kiemTra(request.tuNgay(), request.denNgay());
        // Giữ khoá ghi xếp ca trước mọi khoá khác để không ca nào được tạo xen vào lúc quét ngày nghỉ
        cauHinhService.hienTai();
        cauHinhHeThongRepository.findByIdForUpdate(CauHinhHeThong.ID)
                .orElseThrow(() -> new IllegalStateException("Không có dòng cau_hinh_he_thong"));

        Set<LocalDate> daNghi = ngayNghiBenhVienRepository.timTrongKhoang(request.tuNgay(), request.denNgay())
                .stream().map(NgayNghiBenhVien::getNgay).collect(Collectors.toSet());
        String lyDo = request.lyDo().trim();
        String lyDoEn = request.lyDoEn() == null || request.lyDoEn().isBlank() ? null : request.lyDoEn().trim();
        List<NgayNghiBenhVien> ngayMoi = new ArrayList<>();
        List<LocalDate> ngayDaNghi = new ArrayList<>();
        for (LocalDate ngay = request.tuNgay(); !ngay.isAfter(request.denNgay()); ngay = ngay.plusDays(1)) {
            if (daNghi.contains(ngay)) {
                ngayDaNghi.add(ngay);
                continue;
            }
            NgayNghiBenhVien ngayNghi = new NgayNghiBenhVien();
            ngayNghi.setNgay(ngay);
            ngayNghi.setLyDo(lyDo);
            ngayNghi.setLyDoEn(lyDoEn);
            ngayNghi.setQuanTriTao(quanTri);
            ngayMoi.add(ngayNghi);
        }
        if (ngayMoi.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.NGAY_BENH_VIEN_NGHI, "loi.NGAY_BENH_VIEN_NGHI.daNghiHet");
        }
        ngayNghiBenhVienRepository.saveAll(ngayMoi);

        LyDoHuyCa lyDoHuy = LyDoHuyCa.benhVienNghi(lyDo, lyDoEn);
        List<CaBiHuy> caBiHuy = new ArrayList<>();
        Map<Long, List<LichLamViec>> caTheoBacSi = new LinkedHashMap<>();
        int soLichHenCanDoi = 0;
        // Khoá từng ca theo id; ca vừa bị hủy ở request khác được bỏ qua thay vì ném lỗi
        for (Long idCa : lichLamViecRepository
                .timIdCaHoatDongTrongCacNgay(ngayMoi.stream().map(NgayNghiBenhVien::getNgay).toList())) {
            LichLamViec ca = lichLamViecRepository.findByIdForUpdate(idCa).orElse(null);
            if (ca == null || ca.getTrangThai() != TrangThaiLichLamViec.HOAT_DONG) {
                continue;
            }
            int soLichHen = quanLyCaService.huyCaDaKhoa(ca, quanTri, lyDoHuy, NguonThayDoi.NGAY_NGHI);
            soLichHenCanDoi += soLichHen;
            caBiHuy.add(ngayNghiMapper.toCaBiHuy(ca, soLichHen));
            caTheoBacSi.computeIfAbsent(ca.getBacSi().getId(), id -> new ArrayList<>()).add(ca);
        }

        if (luu) {
            caTheoBacSi.values().forEach(cacCa -> thongBaoLichLamViecService.caBiHuyHangLoat(cacCa.get(0).getBacSi(),
                    cacCa.stream().sorted(THEO_NGAY_GIO).toList(), lyDoHuy));
            log.info("Quản trị viên id={} khai báo bệnh viện nghỉ {} - {}: {} ngày mới, hủy {} ca của {} bác sĩ, {} lịch"
                    + " hẹn cần đổi lịch", quanTri.getId(), request.tuNgay(), request.denNgay(), ngayMoi.size(),
                    caBiHuy.size(), caTheoBacSi.size(), soLichHenCanDoi);
        } else {
            // Xem trước: chạy y như khi lưu rồi rollback, nên kết quả xem trước theo đúng các quy tắc của lần lưu
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return new KetQuaKhaiBaoNgayNghiResponse(luu,
                ngayMoi.stream().map(ngayNghi -> ngayNghiMapper.toQuanTri(ngayNghi, luu)).toList(), ngayDaNghi,
                caBiHuy.stream().sorted(Comparator.comparing(CaBiHuy::ngay).thenComparing(CaBiHuy::gioBatDau)
                        .thenComparing(CaBiHuy::idLichLamViec)).toList(),
                soLichHenCanDoi);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.NGAY_NGHI, allEntries = true)
    public void xoa(Long idTaiKhoanQuanTri, LocalDate ngay) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoanQuanTri);
        if (!ngay.isAfter(LocalDate.now())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.ngayNghiDaQua");
        }
        NgayNghiBenhVien ngayNghi = ngayNghiBenhVienRepository.findByNgay(ngay)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.ngayNghi"));
        ngayNghiBenhVienRepository.delete(ngayNghi);
        log.info("Quản trị viên id={} bỏ ngày nghỉ {} (các ca đã hủy giữ nguyên)", quanTri.getId(), ngay);
    }

}
