package iuh.fit.se.eclinic.identity.service.impl;

import java.nio.charset.StandardCharsets;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.BacSiMoiProperties;
import iuh.fit.se.eclinic.identity.dto.request.SuaThongTinTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.request.TaoTaiKhoanBacSiRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanNoiBoResponse;
import iuh.fit.se.eclinic.identity.event.EmailChaoBacSiEvent;
import iuh.fit.se.eclinic.identity.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.TaiKhoanNoiBoService;
import iuh.fit.se.eclinic.identity.service.TrangThaiTaiKhoanService;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TaiKhoanNoiBoServiceImpl implements TaiKhoanNoiBoService {

    /** Cùng giới hạn với @MatKhauHopLe: mật khẩu mặc định phải là mật khẩu mà người dùng tự đặt cũng được chấp nhận. */
    private static final int DO_DAI_MAT_KHAU_TOI_THIEU = 6;
    private static final int SO_BYTE_MAT_KHAU_TOI_DA = 72;

    private final TaiKhoanRepository taiKhoanRepository;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final TrangThaiTaiKhoanService trangThaiTaiKhoanService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final String matKhauMacDinh;

    /** Kiểm tra cấu hình ngay khi khởi động: mật khẩu mặc định không hợp lệ thì không cho chạy. */
    public TaiKhoanNoiBoServiceImpl(TaiKhoanRepository taiKhoanRepository, BacSiChiDocRepository bacSiChiDocRepository,
            TrangThaiTaiKhoanService trangThaiTaiKhoanService, PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher, BacSiMoiProperties properties) {
        String matKhau = properties.matKhauMacDinh();
        if (matKhau == null || matKhau.length() < DO_DAI_MAT_KHAU_TOI_THIEU
                || matKhau.getBytes(StandardCharsets.UTF_8).length > SO_BYTE_MAT_KHAU_TOI_DA) {
            throw new IllegalStateException("app.bac-si.mat-khau-mac-dinh (DOCTOR_DEFAULT_PASSWORD) phải dài từ "
                    + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự đến " + SO_BYTE_MAT_KHAU_TOI_DA + " byte");
        }
        this.taiKhoanRepository = taiKhoanRepository;
        this.bacSiChiDocRepository = bacSiChiDocRepository;
        this.trangThaiTaiKhoanService = trangThaiTaiKhoanService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.matKhauMacDinh = matKhau;
    }

    @Override
    @Transactional
    public TaiKhoanNoiBoResponse taoTaiKhoanBacSi(TaoTaiKhoanBacSiRequest request) {
        String email = ChuanHoa.email(request.email());
        String soDienThoai = chuanHoaSoDienThoai(request.soDienThoai());
        // Kể cả tài khoản đang chờ kích hoạt: quản trị viên không được chiếm email người khác đang đăng ký
        if (taiKhoanRepository.existsByEmail(email)) {
            throw new LoiNghiepVu(MaLoi.EMAIL_DA_TON_TAI);
        }
        if (soDienThoai != null && taiKhoanRepository.existsBySoDienThoai(soDienThoai)) {
            throw new LoiNghiepVu(MaLoi.SO_DIEN_THOAI_DA_TON_TAI);
        }
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen(request.hoTen().trim());
        taiKhoan.setEmail(email);
        taiKhoan.setSoDienThoai(soDienThoai);
        taiKhoan.setVaiTro(VaiTro.BAC_SI);
        // Quản trị viên chịu trách nhiệm về email: không qua bước xác thực email
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
        taiKhoan.setMatKhauHash(passwordEncoder.encode(matKhauMacDinh));
        taiKhoan.setPhaiDoiMatKhau(true);
        try {
            taiKhoan = taiKhoanRepository.saveAndFlush(taiKhoan);
        } catch (DataIntegrityViolationException e) {
            // 2 request tạo cùng email / số điện thoại cùng lúc: khoá duy nhất của bảng chặn request sau
            throw new LoiNghiepVu(MaLoi.EMAIL_DA_TON_TAI, "Email hoặc số điện thoại đã được đăng ký");
        }
        eventPublisher.publishEvent(new EmailChaoBacSiEvent(email, taiKhoan.getHoTen()));
        log.info("Tạo tài khoản bác sĩ id={} (mật khẩu mặc định, phải đặt mật khẩu ở lần đăng nhập đầu)",
                taiKhoan.getId());
        return toResponse(taiKhoan);
    }

    @Override
    @Transactional
    public void xoaTaiKhoanBacSi(Long id) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(id).orElse(null);
        if (taiKhoan == null) {
            return;
        }
        if (taiKhoan.getVaiTro() != VaiTro.BAC_SI || !taiKhoan.isPhaiDoiMatKhau()
                || bacSiChiDocRepository.existsByTaiKhoanId(id)) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_DANG_DUOC_SU_DUNG);
        }
        taiKhoanRepository.delete(taiKhoan);
        log.info("Xoá tài khoản bác sĩ id={} vì không tạo được hồ sơ bác sĩ", id);
    }

    @Override
    @Transactional
    public TaiKhoanNoiBoResponse suaThongTin(Long id, SuaThongTinTaiKhoanRequest request) {
        TaiKhoan taiKhoan = layTaiKhoanBacSi(id);
        String soDienThoai = chuanHoaSoDienThoai(request.soDienThoai());
        // Kiểm tra TRƯỚC khi sửa entity: truy vấn sau khi sửa sẽ flush thay đổi và vấp khoá duy nhất của bảng
        if (soDienThoai != null && taiKhoanRepository.existsBySoDienThoaiAndIdNot(soDienThoai, id)) {
            throw new LoiNghiepVu(MaLoi.SO_DIEN_THOAI_DA_TON_TAI);
        }
        taiKhoan.setHoTen(request.hoTen().trim());
        taiKhoan.setSoDienThoai(soDienThoai);
        log.info("Sửa họ tên / số điện thoại của tài khoản bác sĩ id={}", id);
        return toResponse(taiKhoan);
    }

    @Override
    @Transactional
    public TaiKhoanNoiBoResponse voHieuHoa(Long id, String lyDo) {
        TaiKhoan taiKhoan = layTaiKhoanBacSi(id);
        if (taiKhoan.getTrangThai() != TrangThaiTaiKhoan.VO_HIEU_HOA) {
            int soPhien = trangThaiTaiKhoanService.voHieuHoa(taiKhoan, lyDo.trim());
            log.info("Vô hiệu hoá tài khoản id={} của bác sĩ ngừng công tác, thu hồi {} phiên", id, soPhien);
        }
        return new TaiKhoanNoiBoResponse(id, TrangThaiTaiKhoan.VO_HIEU_HOA);
    }

    @Override
    @Transactional
    public TaiKhoanNoiBoResponse kichHoatLai(Long id) {
        TaiKhoan taiKhoan = layTaiKhoanBacSi(id);
        if (taiKhoan.getTrangThai() != TrangThaiTaiKhoan.DA_KICH_HOAT) {
            trangThaiTaiKhoanService.kichHoatLai(taiKhoan);
            log.info("Kích hoạt lại tài khoản id={} của bác sĩ công tác lại", id);
        }
        return toResponse(taiKhoan);
    }

    private TaiKhoan layTaiKhoanBacSi(Long id) {
        return taiKhoanRepository.findById(id)
                .filter(taiKhoan -> taiKhoan.getVaiTro() == VaiTro.BAC_SI)
                .orElseThrow(() -> new LoiKhongTimThay("TaiKhoan", id));
    }

    private static String chuanHoaSoDienThoai(String soDienThoai) {
        return soDienThoai == null || soDienThoai.isBlank() ? null : soDienThoai.trim();
    }

    private static TaiKhoanNoiBoResponse toResponse(TaiKhoan taiKhoan) {
        return new TaiKhoanNoiBoResponse(taiKhoan.getId(), taiKhoan.getTrangThai());
    }

}
