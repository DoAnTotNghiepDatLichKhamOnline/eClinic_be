package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.DatLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.QuanTriDatLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.QuanTriDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.QuanTriHuyLichHenRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichHotlineResponse;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenQuanTriChiTietResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.mapper.LichSuLichHenMapper;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichSuLichHenRepository;
import iuh.fit.se.eclinic.booking.service.DatLichService;
import iuh.fit.se.eclinic.booking.service.LichSuLichHenService;
import iuh.fit.se.eclinic.booking.service.LuotKhamService;
import iuh.fit.se.eclinic.booking.service.NguoiThucHien;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.booking.service.QuanTriLichHenService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.MauTimKiem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hủy / đổi chạy READ_COMMITTED như {@link HuyDoiLichServiceImpl}: đổi lịch chạy luôn phần chọn lượt khám của đặt lịch
 * trong cùng transaction. Thứ tự khoá như bệnh nhân đổi lịch: dòng lịch hẹn, lượt khám cũ, rồi lượt của khung giờ mới.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuanTriLichHenServiceImpl implements QuanTriLichHenService {

    private static final String DANG_MA_TRA_CUU = "(?i)ECL-\\d{8}-\\d{4}";
    private static final String DANG_CCCD = "\\d{12}";
    /** Không gửi denNgay: lấy tuNgay + 30 (31 ngày). */
    private static final int SO_NGAY_MAC_DINH = 30;
    private static final int SO_NGAY_TOI_DA = 366;

    private final LichHenRepository lichHenRepository;
    private final LichSuLichHenRepository lichSuLichHenRepository;
    private final QuanLyCaService quanLyCaService;
    private final DatLichService datLichService;
    private final LuotKhamService luotKhamService;
    private final LichSuLichHenService lichSuLichHenService;
    private final ThongBaoLichHenService thongBaoLichHenService;
    private final LichHenMapper lichHenMapper;
    private final LichSuLichHenMapper lichSuLichHenMapper;

    @Override
    public TrangDuLieu<LichHenTrongCaResponse> danhSach(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa,
            Long idBacSi, Long idPhongKham, TrangThaiLichHen trangThai, KenhDatLich kenhDat, Boolean canDoiLich,
            Boolean canDoiChieu, String tuKhoa, int trang, int kichThuoc) {
        LocalDate tu = tuNgay == null ? LocalDate.now() : tuNgay;
        LocalDate den = denNgay == null ? tu.plusDays(SO_NGAY_MAC_DINH) : denNgay;
        KhoangNgay.kiemTra(tu, den, SO_NGAY_TOI_DA);
        String daCat = tuKhoa == null ? "" : tuKhoa.trim();
        String ma = daCat.matches(DANG_MA_TRA_CUU) ? daCat.toUpperCase(Locale.ROOT) : null;
        String cccd = daCat.matches(DANG_CCCD) ? daCat : null;
        String mau = ma == null && cccd == null ? MauTimKiem.chua(daCat) : null;
        return TrangDuLieu.tu(lichHenRepository.timChoQuanTri(tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                idChuyenKhoa, idBacSi, idPhongKham, trangThai, kenhDat, canDoiLich, canDoiChieu, ma, cccd, mau,
                PageRequest.of(trang, kichThuoc)).map(lichHen -> lichHenMapper.toTrongCaResponse(lichHen, true)));
    }

    @Override
    public LichHenQuanTriChiTietResponse chiTiet(Long idLichHen) {
        return toChiTiet(lichHenRepository.timTheoIdKemChiTiet(idLichHen)
                .orElseThrow(QuanTriLichHenServiceImpl::khongTimThay));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LichHenQuanTriChiTietResponse huy(Long idTaiKhoan, Long idLichHen, QuanTriHuyLichHenRequest request) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoan);
        LichHen lichHen = khoaConHuyDoiDuoc(idLichHen);
        lichHen.setTrangThai(TrangThaiLichHen.QUAN_TRI_HUY);
        lichHen.setLyDoHuy(request.lyDo().trim());
        // QUAN_TRI_HUY không còn chiếm khung giờ; lượt của ca đã bị hủy (canDoiLich) giữ DA_HUY
        luotKhamService.traLuot(lichHen);
        lichSuLichHenService.ghi(lichHen, SuKienLichHen.HUY, NguoiThucHien.quanTri(quanTri), lichHen.getLyDoHuy(),
                null);
        thongBaoLichHenService.quanTriHuy(lichHen);
        log.info("Quản trị viên id={} hủy lịch hẹn id={}", quanTri.getId(), idLichHen);
        return chiTiet(idLichHen);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LichHenQuanTriChiTietResponse doiLich(Long idTaiKhoan, Long idLichHen, QuanTriDoiLichRequest request) {
        LocalDateTime bayGio = LocalDateTime.now();
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoan);
        LichHen cu = khoaConHuyDoiDuoc(idLichHen);
        cu.setTrangThai(TrangThaiLichHen.DA_HUY_DO_DOI_LICH);
        luotKhamService.traLuot(cu);
        // Ghi xuống trước khi chọn lượt mới: lịch cũ không còn tính vào trùng giờ và giới hạn số lịch đang giữ
        lichHenRepository.flush();
        // Ngôn ngữ của lịch cũ, không phải của quản trị viên: email đổi lịch gửi cho người đặt
        DatLichResponse moi = datLichService.datLaiTuLichCu(cu, request.idLichLamViec(), request.idChuyenKhoa(),
                request.gioBatDauKhung(), bayGio, cu.getNgonNgu());
        LichHen lichMoi = lichHenRepository.timTheoMaTraCuu(moi.maTraCuu()).orElseThrow();
        lichSuLichHenService.ghi(cu, SuKienLichHen.DOI_LICH, NguoiThucHien.quanTri(quanTri), request.lyDo(),
                lichMoi);
        thongBaoLichHenService.quanTriDoi(cu, lichMoi);
        log.info("Quản trị viên id={} đổi lịch hẹn id={} sang lịch mới {}", quanTri.getId(), idLichHen,
                moi.maTraCuu());
        return toChiTiet(lichMoi);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatLichHotlineResponse datHotline(Long idTaiKhoan, QuanTriDatLichRequest request) {
        QuanTriVien quanTri = quanLyCaService.layQuanTriVien(idTaiKhoan);
        DatLichRequest datLich = new DatLichRequest(request.idLichLamViec(), request.idChuyenKhoa(),
                request.gioBatDauKhung(), request.benhNhan(), request.nguoiGiamHo(), request.lyDoKham(), null, null);
        DatLichResponse phieu = datLichService.datQuaHotline(datLich, quanTri,
                request.ngonNgu() == null ? NgonNgu.VI : request.ngonNgu());
        Long idLichHen = lichHenRepository.timTheoMaTraCuu(phieu.maTraCuu()).orElseThrow().getId();
        log.info("Quản trị viên id={} đặt lịch qua tổng đài: lịch hẹn id={} {}", quanTri.getId(), idLichHen,
                phieu.maTraCuu());
        return new DatLichHotlineResponse(idLichHen, phieu);
    }

    /** Khoá dòng lịch hẹn rồi mới kiểm tra, như bệnh nhân / bác sĩ hủy: thao tác cùng lúc thấy trạng thái mới. */
    private LichHen khoaConHuyDoiDuoc(Long idLichHen) {
        LichHen lichHen = lichHenRepository.findByIdForUpdate(idLichHen)
                .orElseThrow(QuanTriLichHenServiceImpl::khongTimThay);
        if (lichHen.getTrangThai() != TrangThaiLichHen.DA_XAC_NHAN) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_KHONG_HUY_DOI_DUOC);
        }
        if (!conTruocGioKham(lichHen)) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_DA_QUA_GIO);
        }
        return lichHen;
    }

    /** Chưa tới giờ khám, hoặc đang chờ đổi vì ca bị hủy (lịch đó không bao giờ thành KHONG_DEN). */
    private static boolean conTruocGioKham(LichHen lichHen) {
        return lichHen.isCanDoiLich() || lichHen.getKhungGio().getGioBatDau().isAfter(LocalDateTime.now());
    }

    private LichHenQuanTriChiTietResponse toChiTiet(LichHen lichHen) {
        // Chuỗi đổi lịch: lùi theo lichHenCu, tiến theo lịch mới tạo từ lịch này
        List<LichHen> chuoi = new ArrayList<>();
        for (LichHen cu = lichHen.getLichHenCu(); cu != null; cu = cu.getLichHenCu()) {
            chuoi.add(cu);
        }
        Collections.reverse(chuoi);
        chuoi.add(lichHen);
        for (LichHen moi = lichHenRepository.findByLichHenCuId(lichHen.getId()).orElse(null); moi != null;
                moi = lichHenRepository.findByLichHenCuId(moi.getId()).orElse(null)) {
            chuoi.add(moi);
        }
        boolean duocHuyDoi = lichHen.getTrangThai() == TrangThaiLichHen.DA_XAC_NHAN && conTruocGioKham(lichHen);
        return lichSuLichHenMapper.toChiTiet(lichHen, duocHuyDoi, chuoi,
                lichSuLichHenRepository.timTheoLichHen(chuoi.stream().map(LichHen::getId).toList()));
    }

    private static LoiNghiepVu khongTimThay() {
        return new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.lichHen");
    }

}
