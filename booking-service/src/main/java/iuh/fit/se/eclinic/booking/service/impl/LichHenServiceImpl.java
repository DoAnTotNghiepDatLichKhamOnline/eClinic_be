package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.LocLichHen;
import iuh.fit.se.eclinic.booking.dto.request.PhamViLichHen;
import iuh.fit.se.eclinic.booking.dto.response.LichHenChiTietCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.NguoiDatLich;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.mapper.KetQuaKhamMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhAnChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.HoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichHenServiceImpl implements LichHenService {

    /** Lịch hẹn còn phải khám: cùng lượt khám chưa kết thúc thì là lịch "sắp tới". */
    private static final List<TrangThaiLichHen> TRANG_THAI_CON_HIEU_LUC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    /** 4 chữ số (hoặc 6 khi ngày quá đông); lịch hẹn có trước khi có mã dùng id nên có thể dài hơn. */
    private static final Pattern DANG_MA_TRA_CUU = Pattern.compile("(?i)ECL-\\d{8}-\\d{4,7}");
    /** Dạng của TokenNgauNhien.tao(), như PhieuKhamServiceImpl. */
    private static final Pattern DANG_MA_PHIEU_KHAM = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final LichHenRepository lichHenRepository;
    private final HoSoBenhNhanService hoSoBenhNhanService;
    private final HoSoBenhAnChiDocRepository hoSoBenhAnChiDocRepository;
    private final KetQuaKhamMapper ketQuaKhamMapper;
    private final TaiKhoanService taiKhoanService;
    private final LichHenMapper lichHenMapper;

    @Override
    public LichHen layTheoId(Long id) {
        return lichHenRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("LichHen", id));
    }

    @Override
    public List<LichHen> timTheoHoSoBenhNhan(Long hoSoBenhNhanId) {
        return lichHenRepository.findByHoSoBenhNhanIdOrderByNgayTaoDesc(hoSoBenhNhanId);
    }

    @Override
    public List<LichHen> timTheoBacSiVaTrangThai(Long bacSiId, TrangThaiLichHen trangThai) {
        return lichHenRepository.findByBacSiIdAndTrangThaiOrderByNgayTaoDesc(bacSiId, trangThai);
    }

    @Override
    public TrangDuLieu<LichHenCuaToiResponse> lichHenCuaToi(Long idTaiKhoan, LocLichHen loc, PhamViLichHen phamVi,
            int trang, int kichThuoc) {
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        Pageable phanTrang = PageRequest.of(trang, kichThuoc);
        LocalDateTime bayGio = LocalDateTime.now();
        HoSoBenhNhan hoSoCuaToi = hoSoDaLienKet(idTaiKhoan);
        Long idHoSoCuaToi = hoSoCuaToi == null ? null : hoSoCuaToi.getId();
        String cccdCuaToi = hoSoCuaToi == null ? null : hoSoCuaToi.getCccd();
        boolean chiBanThan = phamVi == PhamViLichHen.BAN_THAN;
        boolean chiNguoiKhac = phamVi == PhamViLichHen.NGUOI_KHAC;
        Page<LichHen> lichHen = switch (loc) {
            case TAT_CA -> lichHenRepository.timCuaTaiKhoan(idTaiKhoan, idHoSoCuaToi, cccdCuaToi, chiBanThan,
                    chiNguoiKhac, phanTrang);
            case SAP_TOI -> lichHenRepository.timSapToiCuaTaiKhoan(idTaiKhoan, idHoSoCuaToi, cccdCuaToi, chiBanThan,
                    chiNguoiKhac, TRANG_THAI_CON_HIEU_LUC, bayGio, phanTrang);
            case LICH_SU -> lichHenRepository.timLichSuCuaTaiKhoan(idTaiKhoan, idHoSoCuaToi, cccdCuaToi, chiBanThan,
                    chiNguoiKhac, TRANG_THAI_CON_HIEU_LUC, bayGio, phanTrang);
        };
        return TrangDuLieu.tu(lichHen.map(l -> lichHenMapper.toLichHenCuaToi(l, idHoSoCuaToi, idTaiKhoan)));
    }

    @Override
    public List<LichHenCuaToiResponse> lichCuaToiTrongKhoang(Long idTaiKhoan, LocalDate tuNgay, LocalDate denNgay,
            PhamViLichHen phamVi) {
        KhoangNgay.kiemTra(tuNgay, denNgay);
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        HoSoBenhNhan hoSoCuaToi = hoSoDaLienKet(idTaiKhoan);
        Long idHoSoCuaToi = hoSoCuaToi == null ? null : hoSoCuaToi.getId();
        return lichHenRepository
                .timCuaTaiKhoanTrongKhoang(idTaiKhoan, idHoSoCuaToi, hoSoCuaToi == null ? null : hoSoCuaToi.getCccd(),
                        phamVi == PhamViLichHen.BAN_THAN, phamVi == PhamViLichHen.NGUOI_KHAC, tuNgay.atStartOfDay(),
                        denNgay.plusDays(1).atStartOfDay())
                .stream()
                .map(lichHen -> lichHenMapper.toLichHenCuaToi(lichHen, idHoSoCuaToi, idTaiKhoan))
                .toList();
    }

    @Override
    public LichHenChiTietCuaToiResponse chiTietCuaToi(Long idTaiKhoan, String maPhieuKham) {
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        HoSoBenhNhan hoSoCuaToi = hoSoDaLienKet(idTaiKhoan);
        Long idHoSoCuaToi = hoSoCuaToi == null ? null : hoSoCuaToi.getId();
        LichHen lichHen = lichHenRepository
                .timCuaTaiKhoanTheoMaPhieuKham(idTaiKhoan, idHoSoCuaToi,
                        hoSoCuaToi == null ? null : hoSoCuaToi.getCccd(), maPhieuKham)
                // Cột mã không phân biệt hoa thường: so lại chính xác
                .filter(l -> l.getMaTokenPhieuKham().equals(maPhieuKham))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy lịch hẹn"));
        LichHenCuaToiResponse dong = lichHenMapper.toLichHenCuaToi(lichHen, idHoSoCuaToi, idTaiKhoan);
        // Kết quả khám chỉ cho người khám là chủ tài khoản, hoặc lịch do chính tài khoản này đặt
        boolean duocXemKetQua = lichHen.getTrangThai() == TrangThaiLichHen.DA_HOAN_THANH
                && (dong.laBanThan() || dong.nguoiDat() == NguoiDatLich.TOI);
        KetQuaKhamResponse ketQua = !duocXemKetQua ? null
                : hoSoBenhAnChiDocRepository.timTheoLichHenKemDonThuoc(List.of(lichHen.getId())).stream()
                        .findFirst().map(ketQuaKhamMapper::toResponse).orElse(null);
        return lichHenMapper.toChiTietCuaToi(lichHen, dong, ketQua);
    }

    private HoSoBenhNhan hoSoDaLienKet(Long idTaiKhoan) {
        return hoSoBenhNhanService.timDaLienKetCuaTaiKhoan(idTaiKhoan).orElse(null);
    }

    @Override
    public Optional<LichHen> timTheoMa(String ma) {
        String maGon = boDuongDan(ma == null ? "" : ma.trim());
        if (DANG_MA_TRA_CUU.matcher(maGon).matches()) {
            return lichHenRepository.timTheoMaTraCuu(maGon.toUpperCase(Locale.ROOT));
        }
        if (DANG_MA_PHIEU_KHAM.matcher(maGon).matches()) {
            // Collation của cột không phân biệt hoa thường; mã phiếu khám phải khớp từng ký tự
            return lichHenRepository.timTheoMaPhieuKham(maGon)
                    .filter(timThay -> timThay.getMaTokenPhieuKham().equals(maGon));
        }
        return Optional.empty();
    }

    /** Máy quét QR trả cả link phiếu khám (.../phieu-kham/<mã>): lấy đoạn cuối của đường dẫn, bỏ ?query và #fragment. */
    private static String boDuongDan(String ma) {
        if (!ma.contains("/")) {
            return ma;
        }
        String duongDan = ma.split("[?#]", 2)[0];
        String[] cacDoan = duongDan.split("/");
        for (int i = cacDoan.length - 1; i >= 0; i--) {
            if (!cacDoan[i].isBlank()) {
                return cacDoan[i].trim();
            }
        }
        return "";
    }

    @Override
    @Transactional
    public void danhDauDaDoiChieu(Long idLichHen) {
        layTheoId(idLichHen).setCanDoiChieu(false);
    }

}
