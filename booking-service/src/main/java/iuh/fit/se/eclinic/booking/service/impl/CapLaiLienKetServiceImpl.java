package iuh.fit.se.eclinic.booking.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.PhieuKhamProperties;
import iuh.fit.se.eclinic.booking.dto.request.CapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.request.KhachCapLaiLienKetRequest;
import iuh.fit.se.eclinic.booking.dto.response.CapLaiLienKetResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhAnChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.CapLaiLienKetService;
import iuh.fit.se.eclinic.booking.service.GioiHanCapLaiLienKetService;
import iuh.fit.se.eclinic.booking.service.LichSuLichHenService;
import iuh.fit.se.eclinic.booking.service.NguoiThucHien;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.booking.util.CheThongTin;
import iuh.fit.se.eclinic.common.config.SoKhamProperties;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.enums.CachCapLai;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.thongbao.VanBanHeThong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Log không ghi CCCD, SĐT, email hay mã phiếu khám: chỉ id lịch hẹn / mã tra cứu. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CapLaiLienKetServiceImpl implements CapLaiLienKetService {

    private static final int SO_LICH_HEN_TOI_DA = 50;

    private final LichHenRepository lichHenRepository;
    private final HoSoBenhAnChiDocRepository hoSoBenhAnChiDocRepository;
    private final QuanLyCaService quanLyCaService;
    private final TaiKhoanService taiKhoanService;
    private final GioiHanCapLaiLienKetService gioiHanCapLaiLienKetService;
    private final LichSuLichHenService lichSuLichHenService;
    private final ThongBaoLichHenService thongBaoLichHenService;
    private final PhieuKhamProperties phieuKhamProperties;
    private final SoKhamProperties soKhamProperties;
    private final LichHenMapper lichHenMapper;

    @Override
    public CapLaiLienKetResponse quanTri(Long idTaiKhoan, Long idLichHen, CapLaiLienKetRequest request) {
        NguoiThucHien nguoi = NguoiThucHien.quanTri(quanLyCaService.layQuanTriVien(idTaiKhoan));
        LichHen lichHen = lichHenRepository.timTheoIdKemChiTiet(idLichHen)
                .orElseThrow(CapLaiLienKetServiceImpl::khongTimThay);
        return capLai(lichHen, request, nguoi);
    }

    @Override
    public CapLaiLienKetResponse bacSi(Long idTaiKhoan, Long idLichHen, CapLaiLienKetRequest request) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoan);
        LichHen lichHen = lichHenRepository.timTheoIdKemChiTiet(idLichHen)
                .filter(timThay -> timThay.getBacSi().getId().equals(bacSi.getId()))
                .orElseThrow(CapLaiLienKetServiceImpl::khongTimThay);
        return capLai(lichHen, request, NguoiThucHien.bacSi(bacSi));
    }

    @Override
    public void khach(KhachCapLaiLienKetRequest request, String diaChiIp) {
        String maTraCuu = request.maTraCuu().trim().toUpperCase(Locale.ROOT);
        gioiHanCapLaiLienKetService.ghiNhan(maTraCuu, diaChiIp);
        LichHen lichHen = lichHenRepository.timTheoMaTraCuu(maTraCuu).orElse(null);
        if (lichHen == null) {
            log.info("Khách xin cấp lại liên kết: không có lịch hẹn {}", maTraCuu);
            return;
        }
        LichHen hienHanh = hienHanh(lichHen);
        if (!soDienThoaiKhop(hienHanh, request.soDienThoai().trim())) {
            log.info("Khách xin cấp lại liên kết lịch hẹn id={}: SĐT không khớp", hienHanh.getId());
            return;
        }
        String email = thongBaoLichHenService.emailCapLaiLienKet(hienHanh, lienKetSoKham(hienHanh));
        if (email == null) {
            log.info("Khách xin cấp lại liên kết lịch hẹn id={}: lịch hẹn không có email", hienHanh.getId());
            return;
        }
        lichSuLichHenService.ghi(hienHanh, SuKienLichHen.CAP_LAI_LIEN_KET, NguoiThucHien.KHACH,
                VanBanHeThong.ghi("heThong.capLaiQuaEmail", CheThongTin.email(email)), null);
        log.info("Khách xin cấp lại liên kết lịch hẹn id={}: đã ghi email", hienHanh.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LichHenTrongCaResponse> bacSiTimTheoCccd(Long idTaiKhoan, String cccd) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoan).getId();
        return lichHenRepository.timCuaBacSiTheoCccd(idBacSi, cccd, Limit.of(SO_LICH_HEN_TOI_DA)).stream()
                .map(lichHen -> lichHenMapper.toTrongCaResponse(lichHen, false))
                .toList();
    }

    private CapLaiLienKetResponse capLai(LichHen lichHen, CapLaiLienKetRequest request, NguoiThucHien nguoi) {
        LichHen hienHanh = hienHanh(lichHen);
        if (!cccdKhop(hienHanh, request.cccd())) {
            log.warn("Cấp lại liên kết lịch hẹn id={} bị từ chối: CCCD không khớp (người làm: {})", hienHanh.getId(),
                    nguoi.loai());
            throw new LoiNghiepVu(MaLoi.CCCD_KHONG_KHOP);
        }
        String maSoKham = hoSoBenhAnChiDocRepository.timMaSoKhamTheoLichHen(hienHanh.getId()).orElse(null);
        String lienKetSoKham = maSoKham == null ? null : soKhamProperties.lienKet(maSoKham);
        CapLaiLienKetResponse ketQua;
        String lyDo;
        if (request.cachGui() == CachCapLai.EMAIL) {
            String email = thongBaoLichHenService.emailCapLaiLienKet(hienHanh, lienKetSoKham);
            if (email == null) {
                throw new LoiNghiepVu(MaLoi.KHONG_CO_EMAIL_NHAN);
            }
            String emailDaChe = CheThongTin.email(email);
            lyDo = VanBanHeThong.ghi("heThong.capLaiQuaEmail", emailDaChe);
            ketQua = new CapLaiLienKetResponse(hienHanh.getId(), hienHanh.getMaTraCuu(), CachCapLai.EMAIL, emailDaChe,
                    null, null, null, null);
        } else {
            String maPhieuKham = hienHanh.getMaTokenPhieuKham();
            lyDo = VanBanHeThong.ghi("heThong.capLaiTrenManHinh", null);
            ketQua = new CapLaiLienKetResponse(hienHanh.getId(), hienHanh.getMaTraCuu(), CachCapLai.MAN_HINH, null,
                    maPhieuKham, phieuKhamProperties.lienKet(maPhieuKham), maSoKham, lienKetSoKham);
        }
        lichSuLichHenService.ghi(hienHanh, SuKienLichHen.CAP_LAI_LIEN_KET, nguoi, lyDo, null);
        log.info("Cấp lại liên kết lịch hẹn id={} (chọn id={}) bằng {} (người làm: {})", hienHanh.getId(),
                lichHen.getId(), request.cachGui(), nguoi.loai());
        return ketQua;
    }

    /** Lịch hẹn mới nhất của chuỗi đổi lịch bắt đầu từ lịch hẹn này (chính nó nếu chưa đổi). */
    private LichHen hienHanh(LichHen lichHen) {
        LichHen hienHanh = lichHen;
        for (LichHen moi = lichHenRepository.findByLichHenCuId(hienHanh.getId()).orElse(null); moi != null;
                moi = lichHenRepository.findByLichHenCuId(moi.getId()).orElse(null)) {
            hienHanh = moi;
        }
        return hienHanh;
    }

    private String lienKetSoKham(LichHen lichHen) {
        return hoSoBenhAnChiDocRepository.timMaSoKhamTheoLichHen(lichHen.getId()).map(soKhamProperties::lienKet)
                .orElse(null);
    }

    /** CCCD của hồ sơ bệnh nhân hoặc của người giám hộ trên lịch hẹn; so cả 2 để thời gian không lộ chỗ khớp. */
    private static boolean cccdKhop(LichHen lichHen, String cccd) {
        String cuaGiamHo = lichHen.getNguoiGiamHo() == null ? null : lichHen.getNguoiGiamHo().getCccd();
        boolean khopBenhNhan = bang(lichHen.getHoSoBenhNhan().getCccd(), cccd);
        boolean khopGiamHo = bang(cuaGiamHo, cccd);
        return khopBenhNhan || khopGiamHo;
    }

    /** Cùng thứ tự ưu tiên với hủy / đổi bằng link phiếu khám: SĐT liên hệ, rồi SĐT người giám hộ, rồi SĐT hồ sơ. */
    private static boolean soDienThoaiKhop(LichHen lichHen, String soDienThoai) {
        String daLuu = lichHen.getSoDienThoaiLienHe();
        if (daLuu == null && lichHen.getNguoiGiamHo() != null) {
            daLuu = lichHen.getNguoiGiamHo().getSoDienThoai();
        }
        if (daLuu == null) {
            daLuu = lichHen.getHoSoBenhNhan().getSoDienThoai();
        }
        return bang(daLuu, soDienThoai);
    }

    private static boolean bang(String daLuu, String daNhap) {
        return daLuu != null && MessageDigest.isEqual(daLuu.getBytes(StandardCharsets.UTF_8),
                daNhap.getBytes(StandardCharsets.UTF_8));
    }

    private static LoiNghiepVu khongTimThay() {
        return new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.lichHen");
    }

}
