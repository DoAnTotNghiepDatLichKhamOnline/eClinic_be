package iuh.fit.se.eclinic.identity.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatTrangThaiTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.response.ChiTietTaiKhoanResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanQuanTriResponse;
import iuh.fit.se.eclinic.identity.event.TaiKhoanDaXoaEvent;
import iuh.fit.se.eclinic.identity.mapper.TaiKhoanQuanTriMapper;
import iuh.fit.se.eclinic.identity.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.identity.repository.HoSoBenhNhanChiDocRepository;
import iuh.fit.se.eclinic.identity.repository.LichHenChiDocRepository;
import iuh.fit.se.eclinic.identity.repository.PhienChatChiDocRepository;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.repository.ThongBaoChiDocRepository;
import iuh.fit.se.eclinic.identity.service.QuanLyTaiKhoanService;
import iuh.fit.se.eclinic.identity.service.TaiKhoanService;
import iuh.fit.se.eclinic.identity.service.TrangThaiTaiKhoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Log chỉ ghi id của quản trị viên và của tài khoản bị tác động, không ghi email hay lý do. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuanLyTaiKhoanServiceImpl implements QuanLyTaiKhoanService {

    /** Lịch hẹn còn phải khám: chặn việc vô hiệu hoá bác sĩ (UC-DOCT-03). */
    private static final List<TrangThaiLichHen> LICH_HEN_CON_HIEU_LUC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final TaiKhoanRepository taiKhoanRepository;
    private final TaiKhoanService taiKhoanService;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final HoSoBenhNhanChiDocRepository hoSoBenhNhanChiDocRepository;
    private final LichHenChiDocRepository lichHenChiDocRepository;
    private final ThongBaoChiDocRepository thongBaoChiDocRepository;
    private final PhienChatChiDocRepository phienChatChiDocRepository;
    private final TrangThaiTaiKhoanService trangThaiTaiKhoanService;
    private final ApplicationEventPublisher eventPublisher;
    private final TaiKhoanQuanTriMapper taiKhoanQuanTriMapper;
    private final MessageSource messageSource;

    @Override
    public TrangDuLieu<TaiKhoanQuanTriResponse> timKiem(Long idNguoiThucHien, String tuKhoa, VaiTro vaiTro,
            TrangThaiTaiKhoan trangThai, int trang, int kichThuoc) {
        kiemTraNguoiThucHien(idNguoiThucHien);
        // id là tiêu chí phụ để thứ tự ổn định giữa các trang khi nhiều tài khoản tạo cùng lúc (dữ liệu mẫu)
        Pageable pageable = PageRequest.of(trang, kichThuoc, Sort.by(Sort.Order.desc("ngayTao"), Sort.Order.desc("id")));
        Page<TaiKhoan> ketQua = taiKhoanRepository.timKiem(mauTimKiem(tuKhoa), vaiTro, trangThai, pageable);
        Map<Long, LocalDate> ngaySinh = ngaySinhTheoTaiKhoan(ketQua.getContent());
        return TrangDuLieu.tu(ketQua.map(tk -> taiKhoanQuanTriMapper.toResponse(tk, ngaySinh.get(tk.getId()))));
    }

    @Override
    public ChiTietTaiKhoanResponse layChiTiet(Long idNguoiThucHien, Long id) {
        kiemTraNguoiThucHien(idNguoiThucHien);
        return taoChiTiet(taiKhoanService.layTheoId(id));
    }

    @Override
    @Transactional
    public ChiTietTaiKhoanResponse capNhatTrangThai(Long idNguoiThucHien, Long id,
            CapNhatTrangThaiTaiKhoanRequest request) {
        kiemTraNguoiThucHien(idNguoiThucHien);
        TaiKhoan taiKhoan = taiKhoanService.layTheoId(id);
        kiemTraKhongPhaiQuanTriVien(taiKhoan);
        TrangThaiTaiKhoan trangThaiMoi = request.trangThai();
        if (trangThaiMoi == TrangThaiTaiKhoan.CHO_XAC_NHAN) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "loi.DU_LIEU_KHONG_HOP_LE.trangThaiTaiKhoan");
        }
        // Tài khoản chưa xác thực email: vô hiệu hoá rồi kích hoạt lại sẽ thành kích hoạt mà không qua xác thực email
        if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.CHO_XAC_NHAN) {
            throw new LoiNghiepVu(MaLoi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE, "loi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE.chuaXacThuc");
        }
        if (taiKhoan.getTrangThai() == trangThaiMoi) {
            throw new LoiNghiepVu(MaLoi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE,
                    trangThaiMoi == TrangThaiTaiKhoan.VO_HIEU_HOA ? "loi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE.daVoHieuHoa"
                            : "loi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE.dangHoatDong");
        }
        if (trangThaiMoi == TrangThaiTaiKhoan.VO_HIEU_HOA) {
            voHieuHoa(idNguoiThucHien, taiKhoan, request.lyDo());
        } else {
            kichHoatLai(idNguoiThucHien, taiKhoan);
        }
        // Đọc lại: UPDATE thu hồi phiên đã clear persistence context, entity ở trên không còn được quản lý
        return taoChiTiet(taiKhoanService.layTheoId(id));
    }

    @Override
    @Transactional
    public void xoa(Long idNguoiThucHien, Long id) {
        kiemTraNguoiThucHien(idNguoiThucHien);
        TaiKhoan taiKhoan = taiKhoanService.layTheoId(id);
        kiemTraKhongPhaiQuanTriVien(taiKhoan);
        // Kiểm tra từng bảng để thông điệp nói rõ dữ liệu nào đang giữ tài khoản (khoá ngoại của các bảng này là RESTRICT)
        List<String> dangThamChieu = new ArrayList<>();
        if (bacSiChiDocRepository.existsByTaiKhoanId(id)) {
            dangThamChieu.add(tenDuLieu("duLieu.hoSoBacSi"));
        }
        if (hoSoBenhNhanChiDocRepository.existsByTaiKhoanId(id)) {
            dangThamChieu.add(tenDuLieu("duLieu.hoSoBenhNhan"));
        }
        if (lichHenChiDocRepository.existsByTaiKhoanDatId(id)) {
            dangThamChieu.add(tenDuLieu("duLieu.lichHenDaDat"));
        }
        if (thongBaoChiDocRepository.existsByTaiKhoanId(id)) {
            dangThamChieu.add(tenDuLieu("duLieu.thongBao"));
        }
        if (phienChatChiDocRepository.existsByTaiKhoanId(id)) {
            dangThamChieu.add(tenDuLieu("duLieu.phienChat"));
        }
        if (!dangThamChieu.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_DANG_DUOC_SU_DUNG, "loi.TAI_KHOAN_DANG_DUOC_SU_DUNG.chiTiet", String.join(", ", dangThamChieu));
        }
        String anhDaiDien = taiKhoan.getAnhDaiDien();
        try {
            // Phiên đăng nhập (refresh_token) bị xoá theo nhờ ON DELETE CASCADE. Flush ngay để lỗi khoá ngoại của bảng
            // chưa được kiểm tra ở trên (bảng thêm sau này, hoặc dữ liệu vừa được tạo) bật ra tại đây với mã lỗi đúng.
            taiKhoanRepository.delete(taiKhoan);
            taiKhoanRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_DANG_DUOC_SU_DUNG);
        }
        trangThaiTaiKhoanService.huyMoiLienKet(id);
        eventPublisher.publishEvent(new TaiKhoanDaXoaEvent(id, anhDaiDien));
        log.info("Quản trị viên id={} xoá tài khoản id={}", idNguoiThucHien, id);
    }

    private void voHieuHoa(Long idNguoiThucHien, TaiKhoan taiKhoan, String lyDoNhap) {
        Long id = taiKhoan.getId();
        String lyDo = lyDoNhap == null ? "" : lyDoNhap.trim();
        if (lyDo.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.lyDoVoHieuHoa");
        }
        if (taiKhoan.getVaiTro() == VaiTro.BAC_SI) {
            long soLichHen = lichHenChiDocRepository.countSapToiCuaBacSi(id, LICH_HEN_CON_HIEU_LUC, LocalDateTime.now());
            if (soLichHen > 0) {
                throw new LoiNghiepVu(MaLoi.BAC_SI_CON_LICH_HEN, "loi.BAC_SI_CON_LICH_HEN.soLuong", String.valueOf(soLichHen));
            }
        }
        int soPhien = trangThaiTaiKhoanService.voHieuHoa(taiKhoan, lyDo);
        log.info("Quản trị viên id={} vô hiệu hoá tài khoản id={}, thu hồi {} phiên", idNguoiThucHien, id, soPhien);
    }

    /** Không khôi phục phiên nào: người dùng đăng nhập lại. */
    private void kichHoatLai(Long idNguoiThucHien, TaiKhoan taiKhoan) {
        trangThaiTaiKhoanService.kichHoatLai(taiKhoan);
        log.info("Quản trị viên id={} kích hoạt lại tài khoản id={}", idNguoiThucHien, taiKhoan.getId());
    }

    /** Xem javadoc của {@link QuanLyTaiKhoanService}. */
    private void kiemTraNguoiThucHien(Long idNguoiThucHien) {
        if (taiKhoanService.layDangHoatDong(idNguoiThucHien).getVaiTro() != VaiTro.QUAN_TRI_VIEN) {
            throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN);
        }
    }

    /** Tên 1 loại dữ liệu theo ngôn ngữ của request, để ghép vào thông điệp lỗi. */
    private String tenDuLieu(String khoa) {
        return messageSource.getMessage(khoa, null, khoa, NgonNgu.hienTai().locale());
    }

    private static void kiemTraKhongPhaiQuanTriVien(TaiKhoan taiKhoan) {
        if (taiKhoan.getVaiTro() == VaiTro.QUAN_TRI_VIEN) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE);
        }
    }

    /** Chi tiết theo vai trò: quản trị viên không có hồ sơ riêng nên không truy vấn gì thêm. */
    private ChiTietTaiKhoanResponse taoChiTiet(TaiKhoan taiKhoan) {
        BacSi bacSi = taiKhoan.getVaiTro() == VaiTro.BAC_SI
                ? bacSiChiDocRepository.findByTaiKhoanId(taiKhoan.getId()).orElse(null)
                : null;
        HoSoBenhNhan hoSoBenhNhan = taiKhoan.getVaiTro() == VaiTro.BENH_NHAN
                ? hoSoBenhNhanChiDocRepository.findByTaiKhoanId(taiKhoan.getId()).orElse(null)
                : null;
        return taiKhoanQuanTriMapper.toChiTiet(taiKhoan, bacSi, hoSoBenhNhan);
    }

    /** Ngày sinh (từ hồ sơ bệnh nhân đã liên kết) của các tài khoản bệnh nhân trong 1 trang, bằng 1 truy vấn. */
    private Map<Long, LocalDate> ngaySinhTheoTaiKhoan(List<TaiKhoan> taiKhoan) {
        List<Long> idBenhNhan = taiKhoan.stream()
                .filter(tk -> tk.getVaiTro() == VaiTro.BENH_NHAN)
                .map(TaiKhoan::getId)
                .toList();
        Map<Long, LocalDate> ketQua = new HashMap<>();
        if (idBenhNhan.isEmpty()) {
            return ketQua;
        }
        for (HoSoBenhNhan hoSo : hoSoBenhNhanChiDocRepository.findByTaiKhoanIdInAndTrangThaiLienKet(idBenhNhan,
                TrangThaiLienKet.DA_LIEN_KET)) {
            ketQua.put(hoSo.getTaiKhoan().getId(), hoSo.getNgaySinh());
        }
        return ketQua;
    }

    /**
     * Từ khoá -> mẫu LIKE "%...%"; %, _ và ký tự thoát trong từ khoá được thoát để so khớp đúng ký tự đó (gõ "%" không
     * liệt kê mọi tài khoản). null nếu không có từ khoá.
     */
    private static String mauTimKiem(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return null;
        }
        char thoat = TaiKhoanRepository.KY_TU_THOAT;
        StringBuilder mau = new StringBuilder("%");
        for (char kyTu : tuKhoa.trim().toCharArray()) {
            if (kyTu == '%' || kyTu == '_' || kyTu == thoat) {
                mau.append(thoat);
            }
            mau.append(kyTu);
        }
        return mau.append('%').toString();
    }

}
