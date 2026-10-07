package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.DanhGiaProperties;
import iuh.fit.se.eclinic.booking.dto.request.DanhGiaRequest;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaBacSiResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.DanhGiaQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.TongQuanDanhGiaResponse;
import iuh.fit.se.eclinic.booking.mapper.DanhGiaMapper;
import iuh.fit.se.eclinic.booking.repository.DanhGiaRepository;
import iuh.fit.se.eclinic.booking.repository.DanhGiaRepository.SoDanhGiaTheoSao;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.DanhGiaService;
import iuh.fit.se.eclinic.booking.service.HoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.DanhGia;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.DiemDanhGia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DanhGiaServiceImpl implements DanhGiaService {

    private final DanhGiaRepository danhGiaRepository;
    private final LichHenRepository lichHenRepository;
    private final HoSoBenhNhanService hoSoBenhNhanService;
    private final TaiKhoanService taiKhoanService;
    private final DanhGiaMapper danhGiaMapper;
    private final DanhGiaProperties danhGiaProperties;

    @Override
    @Transactional
    public DanhGiaCuaToiResponse gui(Long idTaiKhoan, String maPhieuKham, DanhGiaRequest request) {
        TaiKhoan taiKhoan = taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        LichHen lichHen = lichHenDuocDanhGia(idTaiKhoan, maPhieuKham);
        if (lichHen.getTrangThai() != TrangThaiLichHen.DA_HOAN_THANH) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_CHUA_KHAM_XONG);
        }
        if (danhGiaRepository.existsByLichHenId(lichHen.getId())) {
            throw new LoiNghiepVu(MaLoi.DA_DANH_GIA);
        }
        DanhGia danhGia = new DanhGia();
        danhGia.setLichHen(lichHen);
        danhGia.setBacSi(lichHen.getBacSi());
        danhGia.setTaiKhoan(taiKhoan);
        danhGia.setSoSao(request.soSao());
        danhGia.setNhanXet(rongThanhNull(request.nhanXet()));
        try {
            danhGiaRepository.saveAndFlush(danhGia);
        } catch (DataIntegrityViolationException e) {
            // 2 request gửi cùng lúc: request sau vi phạm UNIQUE(id_lich_hen)
            throw new LoiNghiepVu(MaLoi.DA_DANH_GIA);
        }
        log.info("Tài khoản id={} đánh giá lịch hẹn id={}: {} sao", idTaiKhoan, lichHen.getId(), request.soSao());
        return danhGiaMapper.toCuaToi(danhGia);
    }

    @Override
    @Transactional
    public DanhGiaCuaToiResponse sua(Long idTaiKhoan, String maPhieuKham, DanhGiaRequest request) {
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        LichHen lichHen = lichHenDuocDanhGia(idTaiKhoan, maPhieuKham);
        DanhGia danhGia = danhGiaRepository.findByLichHenId(lichHen.getId())
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Lịch hẹn này chưa có đánh giá"));
        if (LocalDateTime.now().isAfter(danhGia.getNgayTao().plusDays(danhGiaProperties.soNgayDuocSua()))) {
            throw new LoiNghiepVu(MaLoi.HET_HAN_SUA_DANH_GIA);
        }
        danhGia.setSoSao(request.soSao());
        danhGia.setNhanXet(rongThanhNull(request.nhanXet()));
        danhGiaRepository.saveAndFlush(danhGia);
        log.info("Tài khoản id={} sửa đánh giá của lịch hẹn id={}: {} sao", idTaiKhoan, lichHen.getId(),
                request.soSao());
        return danhGiaMapper.toCuaToi(danhGia);
    }

    @Override
    public TongQuanDanhGiaResponse tongQuanCuaBacSi(Long idTaiKhoanBacSi) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        Map<Integer, Long> phanBo = new TreeMap<>();
        for (int sao = 1; sao <= 5; sao++) {
            phanBo.put(sao, 0L);
        }
        long soDanhGia = 0;
        long tongSao = 0;
        for (SoDanhGiaTheoSao muc : danhGiaRepository.demTheoSao(idBacSi)) {
            phanBo.put(muc.getSoSao(), muc.getSoLuong());
            soDanhGia += muc.getSoLuong();
            tongSao += muc.getSoSao() * muc.getSoLuong();
        }
        return new TongQuanDanhGiaResponse(
                soDanhGia == 0 ? null : DiemDanhGia.lamTron((double) tongSao / soDanhGia), soDanhGia, phanBo);
    }

    @Override
    public TrangDuLieu<DanhGiaCuaBacSiResponse> cuaBacSi(Long idTaiKhoanBacSi, int trang, int kichThuoc) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        return TrangDuLieu.tu(danhGiaRepository.timCuaBacSi(idBacSi, PageRequest.of(trang, kichThuoc))
                .map(danhGiaMapper::toCuaBacSi));
    }

    @Override
    public TrangDuLieu<DanhGiaQuanTriResponse> choQuanTri(Long idBacSi, Integer soSaoToiDa, int trang,
            int kichThuoc) {
        return TrangDuLieu.tu(danhGiaRepository.timChoQuanTri(idBacSi, soSaoToiDa, PageRequest.of(trang, kichThuoc))
                .map(danhGiaMapper::toQuanTri));
    }

    /**
     * Lịch hẹn tài khoản nhìn thấy (như "lịch hẹn của tôi") VÀ được xem kết quả khám: chủ tài khoản là người khám, hoặc
     * tài khoản đã đặt lịch. Tài khoản chỉ thấy lịch vì là người giám hộ theo CCCD thì không được đánh giá.
     */
    private LichHen lichHenDuocDanhGia(Long idTaiKhoan, String maPhieuKham) {
        HoSoBenhNhan hoSoCuaToi = hoSoBenhNhanService.timDaLienKetCuaTaiKhoan(idTaiKhoan).orElse(null);
        Long idHoSoCuaToi = hoSoCuaToi == null ? null : hoSoCuaToi.getId();
        LichHen lichHen = lichHenRepository
                .timCuaTaiKhoanTheoMaPhieuKham(idTaiKhoan, idHoSoCuaToi,
                        hoSoCuaToi == null ? null : hoSoCuaToi.getCccd(), maPhieuKham)
                // Cột mã không phân biệt hoa thường: so lại chính xác
                .filter(l -> l.getMaTokenPhieuKham().equals(maPhieuKham))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy lịch hẹn"));
        boolean laNguoiKham = lichHen.getHoSoBenhNhan().getId().equals(idHoSoCuaToi);
        boolean laNguoiDat = lichHen.getTaiKhoanDat() != null && lichHen.getTaiKhoanDat().getId().equals(idTaiKhoan);
        if (!laNguoiKham && !laNguoiDat) {
            throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN, "Chỉ người khám hoặc người đã đặt lịch mới đánh giá được");
        }
        return lichHen;
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }
}
