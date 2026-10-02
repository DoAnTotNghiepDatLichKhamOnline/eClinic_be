package iuh.fit.se.eclinic.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.BaoMatProperties;
import iuh.fit.se.eclinic.common.security.JwtService;
import iuh.fit.se.eclinic.identity.config.DangNhapProperties;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.mapper.TaiKhoanMapper;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.GioiHanDangNhapService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService.PhienMoi;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import iuh.fit.se.eclinic.identity.util.TokenNgauNhien;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class DangNhapServiceImpl implements DangNhapService {

    /**
     * BCrypt chỉ đọc 72 byte đầu: matches() KHÔNG báo lỗi mà cắt bớt, nên "mật khẩu đúng + ký tự bất kỳ" cũng khớp.
     * Mật khẩu dài hơn không thể là mật khẩu đã đăng ký (@MatKhauHopLe chặn khi đăng ký).
     */
    private static final int SO_BYTE_MAT_KHAU_TOI_DA = 72;
    private static final String LOAI_TOKEN = "Bearer";

    private final TaiKhoanRepository taiKhoanRepository;
    private final RefreshTokenService refreshTokenService;
    private final GioiHanDangNhapService gioiHanDangNhapService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BaoMatProperties baoMatProperties;
    private final DangNhapProperties dangNhapProperties;
    private final TaiKhoanMapper taiKhoanMapper;

    /** Hash giả để vẫn chạy BCrypt khi email không tồn tại: thời gian phản hồi không lộ email nào đã đăng ký. */
    private final String hashGia;

    public DangNhapServiceImpl(TaiKhoanRepository taiKhoanRepository, RefreshTokenService refreshTokenService,
            GioiHanDangNhapService gioiHanDangNhapService, PasswordEncoder passwordEncoder, JwtService jwtService,
            BaoMatProperties baoMatProperties, DangNhapProperties dangNhapProperties, TaiKhoanMapper taiKhoanMapper) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.refreshTokenService = refreshTokenService;
        this.gioiHanDangNhapService = gioiHanDangNhapService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.baoMatProperties = baoMatProperties;
        this.dangNhapProperties = dangNhapProperties;
        this.taiKhoanMapper = taiKhoanMapper;
        this.hashGia = passwordEncoder.encode(TokenNgauNhien.tao());
    }

    @Override
    @Transactional
    public DangNhapResponse dangNhap(DangNhapRequest request, String thongTinThietBi) {
        String email = ChuanHoa.email(request.email());
        // Đang bị khoá thì từ chối ngay, không chạy BCrypt (429 không phải bí mật)
        if (gioiHanDangNhapService.dangBiKhoa(email)) {
            throw new LoiNghiepVu(MaLoi.DANG_NHAP_SAI_QUA_NHIEU);
        }

        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(email).orElse(null);
        if (!khopMatKhau(taiKhoan, request.matKhau())) {
            long soLanSai = gioiHanDangNhapService.ghiNhanThatBai(email);
            // Nhật ký bảo mật: chỉ ghi id ("-" khi email chưa đăng ký), không ghi email / mật khẩu
            String id = taiKhoan != null ? taiKhoan.getId().toString() : "-";
            log.info("Đăng nhập sai id={} (lần sai thứ {})", id, soLanSai);
            if (soLanSai >= dangNhapProperties.soLanSaiToiDa()) {
                log.warn("Khoá đăng nhập id={} sau {} lần sai mật khẩu", id, soLanSai);
            }
            throw new LoiNghiepVu(MaLoi.SAI_THONG_TIN_DANG_NHAP);
        }

        // Mật khẩu đúng: xoá bộ đếm rồi mới báo trạng thái tài khoản
        gioiHanDangNhapService.xoa(email);
        switch (taiKhoan.getTrangThai()) {
            case CHO_XAC_NHAN -> {
                log.info("Từ chối đăng nhập id={}: tài khoản chưa kích hoạt", taiKhoan.getId());
                throw new LoiNghiepVu(MaLoi.TAI_KHOAN_CHUA_XAC_THUC);
            }
            case VO_HIEU_HOA -> {
                log.info("Từ chối đăng nhập id={}: tài khoản bị vô hiệu hoá", taiKhoan.getId());
                throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
            }
            case DA_KICH_HOAT -> {
                // Được đăng nhập
            }
        }
        log.info("Đăng nhập thành công id={}", taiKhoan.getId());
        return capPhien(taiKhoan, thongTinThietBi);
    }

    /**
     * Không rollback khi ném LoiNghiepVu: mọi thao tác ghi trước khi ném lỗi đều là thu hồi phiên và phải được giữ
     * (thu hồi phiên khi token cũ của nó bị dùng lại, thu hồi phiên của tài khoản bị vô hiệu hoá).
     * Chỉ đúng khi LoiNghiepVu được ném ở thân method này: nếu method @Transactional bên trong (RefreshTokenService)
     * tự ném thì transaction chung bị đánh dấu rollback.
     */
    @Override
    @Transactional(noRollbackFor = LoiNghiepVu.class)
    public DangNhapResponse lamMoi(String refreshToken) {
        RefreshToken phien = refreshTokenService.timTheoToken(refreshToken)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE));
        // Đọc hết trước khi thu hồi: UPDATE (clearAutomatically) làm entity bị detach, taiKhoan là proxy lazy
        Long idPhien = phien.getId();
        Long idTaiKhoan = phien.getTaiKhoan().getId();
        String thongTinThietBi = phien.getThongTinThietBi();
        String maPhien = phien.getMaPhien();
        LocalDateTime ngayDangNhap = phien.getNgayDangNhap();
        LocalDateTime bayGio = LocalDateTime.now();

        // Hết hạn thì chỉ từ chối, kể cả khi đã thu hồi: token cũ hàng tuần không phải dấu hiệu bị lộ
        if (!phien.getNgayHetHan().isAfter(bayGio)) {
            throw new LoiNghiepVu(MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        }
        if (phien.getNgayThuHoi() != null) {
            // Quá thời gian ân hạn: token đã xoay vòng bị dùng lại -> có thể đã bị lộ, đăng xuất phiên của token đó
            // (chỉ phiên đó: thiết bị bị đăng xuất từ xa quay lại với cookie cũ không được làm văng thiết bị khác).
            // Trong ân hạn: thường là 2 tab làm mới cùng lúc, chỉ từ chối.
            if (phien.getNgayThuHoi().plus(dangNhapProperties.anHanDungLai()).isBefore(bayGio)) {
                if (refreshTokenService.thuHoiPhien(idTaiKhoan, maPhien)) {
                    log.warn("Refresh token đã thu hồi bị dùng lại: đăng xuất phiên {} của tài khoản id={}", maPhien,
                            idTaiKhoan);
                } else {
                    // Phiên đã đăng xuất (tự đăng xuất, bị đăng xuất từ xa, đổi mật khẩu): không còn gì để thu hồi
                    log.info("Refresh token của phiên đã đăng xuất bị dùng lại, tài khoản id={}", idTaiKhoan);
                }
            }
            throw new LoiNghiepVu(MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        }
        // Chỉ 1 request giành được phiên; request đồng thời còn lại nhận 401 như trường hợp ân hạn
        if (!refreshTokenService.thuHoiNeuConHieuLuc(idPhien)) {
            throw new LoiNghiepVu(MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        }

        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE));
        switch (taiKhoan.getTrangThai()) {
            case VO_HIEU_HOA -> throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
            case CHO_XAC_NHAN -> throw new LoiNghiepVu(MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
            case DA_KICH_HOAT -> {
                // Được cấp phiên mới
            }
        }
        log.info("Làm mới phiên id={} của tài khoản id={}", idPhien, idTaiKhoan);
        // Cùng 1 phiên: giữ mã phiên và thời điểm đăng nhập, chỉ đổi token
        String refreshTokenMoi = refreshTokenService.taoTiepTheo(taiKhoan, maPhien, ngayDangNhap, thongTinThietBi,
                dangNhapProperties.thoiHanRefreshToken());
        return taoKetQua(taiKhoan, maPhien, refreshTokenMoi);
    }

    @Override
    @Transactional
    public void dangXuat(String refreshToken) {
        refreshTokenService.timTheoToken(refreshToken)
                .filter(phien -> refreshTokenService.thuHoiNeuConHieuLuc(phien.getId()))
                .ifPresent(phien -> log.info("Đăng xuất phiên id={}", phien.getId()));
    }

    /** Luôn chạy BCrypt đúng 1 lần, kể cả khi không có tài khoản hoặc mật khẩu quá dài. */
    private boolean khopMatKhau(TaiKhoan taiKhoan, String matKhau) {
        boolean hopLe = taiKhoan != null && taiKhoan.getMatKhauHash() != null
                && matKhau.getBytes(StandardCharsets.UTF_8).length <= SO_BYTE_MAT_KHAU_TOI_DA;
        boolean khop = passwordEncoder.matches(matKhau, hopLe ? taiKhoan.getMatKhauHash() : hashGia);
        return hopLe && khop;
    }

    @Override
    @Transactional
    public DangNhapResponse capPhien(TaiKhoan taiKhoan, String thongTinThietBi) {
        PhienMoi phien = refreshTokenService.tao(taiKhoan, thongTinThietBi, dangNhapProperties.thoiHanRefreshToken());
        return taoKetQua(taiKhoan, phien.maPhien(), phien.refreshToken());
    }

    /** Access token mang mã phiên (claim "phien") để các API /api/users/me biết request đến từ phiên nào. */
    private DangNhapResponse taoKetQua(TaiKhoan taiKhoan, String maPhien, String refreshToken) {
        String accessToken = jwtService.taoAccessToken(taiKhoan.getId(), taiKhoan.getEmail(), taiKhoan.getVaiTro(),
                maPhien);
        return new DangNhapResponse(accessToken, refreshToken, LOAI_TOKEN,
                baoMatProperties.thoiHanAccessToken().toSeconds(), taiKhoanMapper.toResponse(taiKhoan));
    }

}
