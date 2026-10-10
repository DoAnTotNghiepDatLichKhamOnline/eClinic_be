package iuh.fit.se.eclinic.booking.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.dto.request.DoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.HuyLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.DatLichService;
import iuh.fit.se.eclinic.booking.service.HoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.service.HuyDoiLichService;
import iuh.fit.se.eclinic.booking.service.LuotKhamService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.NguoiGiamHo;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * READ_COMMITTED như {@link DatLichServiceImpl#datLich}: đổi lịch chạy luôn phần chọn lượt khám của đặt lịch trong cùng
 * transaction này.
 * <p>
 * Thứ tự khoá: dòng lịch hẹn cũ, lượt khám cũ, rồi các lượt của khung giờ mới (như đặt lịch). 2 người đổi chéo khung giờ
 * của nhau cùng lúc có thể deadlock: MySQL huỷ 1 giao dịch, XuLyLoiHandler trả lỗi "thử lại".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HuyDoiLichServiceImpl implements HuyDoiLichService {

    /** Dạng của TokenNgauNhien.tao(), như PhieuKhamServiceImpl. */
    private static final Pattern DANG_MA_PHIEU_KHAM = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern DANG_SO_DIEN_THOAI = Pattern.compile("0\\d{9}");

    private static final List<TrangThaiLichHen> TRANG_THAI_HUY_DOI_DUOC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final LichHenRepository lichHenRepository;
    private final LuotKhamService luotKhamService;
    private final TaiKhoanService taiKhoanService;
    private final HoSoBenhNhanService hoSoBenhNhanService;
    private final DatLichService datLichService;
    private final DatLichProperties datLichProperties;
    private final LichHenMapper lichHenMapper;
    private final ThongBaoLichHenService thongBaoLichHenService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PhieuKhamResponse huyCuaToi(Long idTaiKhoan, String maPhieuKham, String lyDo) {
        LichHen lichHen = khoa(maPhieuKham);
        kiemTraQuyenCuaTaiKhoan(lichHen, idTaiKhoan);
        return huy(lichHen, lyDo, "tài khoản id=" + idTaiKhoan);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatLichResponse doiCuaToi(Long idTaiKhoan, String maPhieuKham, DoiLichRequest request) {
        LichHen lichHen = khoa(maPhieuKham);
        kiemTraQuyenCuaTaiKhoan(lichHen, idTaiKhoan);
        return doi(lichHen, request, "tài khoản id=" + idTaiKhoan);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PhieuKhamResponse huyTheoPhieu(String maPhieuKham, HuyLichRequest request) {
        LichHen lichHen = khoa(maPhieuKham);
        kiemTraSoDienThoai(lichHen, request.soDienThoai());
        return huy(lichHen, request.lyDo(), "link phiếu khám");
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatLichResponse doiTheoPhieu(String maPhieuKham, DoiLichRequest request) {
        LichHen lichHen = khoa(maPhieuKham);
        kiemTraSoDienThoai(lichHen, request.soDienThoai());
        return doi(lichHen, request, "link phiếu khám");
    }

    private PhieuKhamResponse huy(LichHen lichHen, String lyDo, String nguoiLam) {
        kiemTraConHuyDoiDuoc(lichHen, LocalDateTime.now());
        lichHen.setTrangThai(TrangThaiLichHen.DA_HUY);
        lichHen.setLyDoHuy(lyDo == null || lyDo.isBlank() ? null : lyDo.trim());
        traLuotKham(lichHen);
        thongBaoLichHenService.benhNhanDaHuy(lichHen);
        log.info("Hủy lịch hẹn id={} qua {}", lichHen.getId(), nguoiLam);
        // Tải lại kèm chi tiết cho mapper; cùng transaction nên vẫn là dòng vừa đổi trạng thái
        return lichHenMapper.toPhieuKhamResponse(
                lichHenRepository.timTheoMaPhieuKham(lichHen.getMaTokenPhieuKham()).orElse(lichHen));
    }

    private DatLichResponse doi(LichHen lichHen, DoiLichRequest request, String nguoiLam) {
        LocalDateTime bayGio = LocalDateTime.now();
        kiemTraConHuyDoiDuoc(lichHen, bayGio);
        int toiDa = datLichProperties.soLanDoiLichToiDa();
        // Lịch đang chờ đổi vì ca khám bị hủy: không phải bệnh nhân muốn đổi, nên không tính vào giới hạn
        if (!lichHen.isCanDoiLich() && soLanDaDoi(lichHen) >= toiDa) {
            throw new LoiNghiepVu(MaLoi.VUOT_SO_LAN_DOI_LICH,
                    "Lịch hẹn này đã được đổi " + toiDa + " lần, không đổi thêm được; bạn vẫn có thể hủy lịch");
        }
        lichHen.setTrangThai(TrangThaiLichHen.DA_HUY_DO_DOI_LICH);
        traLuotKham(lichHen);
        // Ghi xuống trước khi chọn lượt mới: lịch cũ không còn tính vào trùng giờ và giới hạn số lịch đang giữ
        lichHenRepository.flush();
        DatLichResponse moi = datLichService.datLaiTuLichCu(lichHen, request.idLichLamViec(), request.idChuyenKhoa(),
                request.gioBatDauKhung(), bayGio);
        thongBaoLichHenService.benhNhanDaDoi(lichHen, lichHenRepository.timTheoMaTraCuu(moi.maTraCuu()).orElseThrow());
        log.info("Đổi lịch hẹn id={} sang lịch mới {} qua {}", lichHen.getId(), moi.maTraCuu(), nguoiLam);
        return moi;
    }

    /**
     * Khoá dòng lịch hẹn rồi mới kiểm tra, để bấm 2 lần (hoặc bác sĩ từ chối cùng lúc) thì lần sau thấy trạng thái mới.
     * Mã sai dạng và mã không tồn tại trả cùng 1 lỗi.
     */
    private LichHen khoa(String maPhieuKham) {
        if (maPhieuKham == null || !DANG_MA_PHIEU_KHAM.matcher(maPhieuKham).matches()) {
            throw khongTimThay();
        }
        return lichHenRepository.findByMaPhieuKhamForUpdate(maPhieuKham)
                // Collation của cột không phân biệt hoa thường; mã phải khớp từng ký tự
                .filter(lichHen -> lichHen.getMaTokenPhieuKham().equals(maPhieuKham))
                .orElseThrow(HuyDoiLichServiceImpl::khongTimThay);
    }

    private static LoiNghiepVu khongTimThay() {
        return new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy lịch hẹn");
    }

    /** Cùng quy tắc với "được xem kết quả khám" (LichHenRepository#DUOC_XEM_KET_QUA). */
    private void kiemTraQuyenCuaTaiKhoan(LichHen lichHen, Long idTaiKhoan) {
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        HoSoBenhNhan hoSoCuaToi = hoSoBenhNhanService.timDaLienKetCuaTaiKhoan(idTaiKhoan).orElse(null);
        boolean toiDat = lichHen.getTaiKhoanDat() != null && lichHen.getTaiKhoanDat().getId().equals(idTaiKhoan);
        boolean laBanThan = hoSoCuaToi != null && lichHen.getHoSoBenhNhan().getId().equals(hoSoCuaToi.getId());
        if (toiDat || laBanThan) {
            return;
        }
        NguoiGiamHo nguoiGiamHo = lichHen.getNguoiGiamHo();
        boolean laNguoiGiamHo = hoSoCuaToi != null && hoSoCuaToi.getCccd() != null && nguoiGiamHo != null
                && hoSoCuaToi.getCccd().equals(nguoiGiamHo.getCccd());
        if (laNguoiGiamHo) {
            throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN,
                    "Chỉ người đã đặt lịch hoặc người khám mới hủy / đổi được lịch hẹn này");
        }
        throw khongTimThay();
    }

    /**
     * SĐT liên hệ của lượt khám; lịch hẹn tạo trước khi có cột này thì lấy SĐT người giám hộ, rồi SĐT của hồ sơ. So sánh
     * không phụ thuộc vị trí ký tự sai (không lộ thông tin qua thời gian phản hồi).
     */
    private static void kiemTraSoDienThoai(LichHen lichHen, String soDienThoai) {
        String daNhap = soDienThoai == null ? "" : soDienThoai.trim();
        if (!DANG_SO_DIEN_THOAI.matcher(daNhap).matches()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "Nhập số điện thoại đã dùng khi đặt lịch (10 chữ số, bắt đầu bằng 0)");
        }
        String daLuu = lichHen.getSoDienThoaiLienHe();
        if (daLuu == null && lichHen.getNguoiGiamHo() != null) {
            daLuu = lichHen.getNguoiGiamHo().getSoDienThoai();
        }
        if (daLuu == null) {
            daLuu = lichHen.getHoSoBenhNhan().getSoDienThoai();
        }
        if (daLuu == null || !MessageDigest.isEqual(daLuu.getBytes(StandardCharsets.UTF_8),
                daNhap.getBytes(StandardCharsets.UTF_8))) {
            throw new LoiNghiepVu(MaLoi.SO_DIEN_THOAI_KHONG_KHOP);
        }
    }

    private void kiemTraConHuyDoiDuoc(LichHen lichHen, LocalDateTime bayGio) {
        if (!TRANG_THAI_HUY_DOI_DUOC.contains(lichHen.getTrangThai())) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_KHONG_HUY_DOI_DUOC);
        }
        if (!bayGio.isBefore(lichHenMapper.hanHuyDoi(lichHen))) {
            throw new LoiNghiepVu(MaLoi.QUA_HAN_HUY_DOI_LICH);
        }
    }

    /** Số lần lịch hẹn này đã được đổi: số lịch cũ đứng trước nó trong chuỗi {@code lichHenCu}. */
    private static int soLanDaDoi(LichHen lichHen) {
        int soLan = 0;
        for (LichHen cu = lichHen.getLichHenCu(); cu != null; cu = cu.getLichHenCu()) {
            soLan++;
        }
        return soLan;
    }

    private void traLuotKham(LichHen lichHen) {
        luotKhamService.traLuot(lichHen);
    }

}
