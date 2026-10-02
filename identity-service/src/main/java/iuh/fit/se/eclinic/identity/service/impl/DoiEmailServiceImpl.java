package iuh.fit.se.eclinic.identity.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.LienKetProperties;
import iuh.fit.se.eclinic.identity.dto.request.DoiEmailRequest;
import iuh.fit.se.eclinic.identity.dto.response.YeuCauDoiEmailResponse;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.event.EmailDaDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacNhanDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailYeuCauDoiEmailEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DoiEmailService;
import iuh.fit.se.eclinic.identity.service.GioiHanDangNhapService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.service.TaiKhoanService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService.LienKetDaDung;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Log chỉ ghi id tài khoản, không ghi địa chỉ email hay token. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoiEmailServiceImpl implements DoiEmailService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final TaiKhoanService taiKhoanService;
    private final MatKhauService matKhauService;
    private final TokenLienKetService tokenLienKetService;
    private final RefreshTokenService refreshTokenService;
    private final GioiHanDangNhapService gioiHanDangNhapService;
    private final ApplicationEventPublisher eventPublisher;
    private final LienKetProperties lienKetProperties;

    @Override
    public YeuCauDoiEmailResponse yeuCau(Long idTaiKhoan, DoiEmailRequest request) {
        TaiKhoan taiKhoan = taiKhoanService.layDangHoatDong(idTaiKhoan);
        matKhauService.xacNhanMatKhauHienTai(taiKhoan, request.matKhauHienTai());
        String emailMoi = ChuanHoa.email(request.emailMoi());
        if (emailMoi.equals(taiKhoan.getEmail())) {
            throw new LoiNghiepVu(MaLoi.EMAIL_MOI_TRUNG_EMAIL_CU);
        }
        if (taiKhoanRepository.existsByEmail(emailMoi)) {
            throw new LoiNghiepVu(MaLoi.EMAIL_DA_TON_TAI);
        }
        if (!tokenLienKetService.giuCho(idTaiKhoan, MucDichLienKet.DOI_EMAIL, lienKetProperties.thoiGianChoGuiLai())) {
            throw new LoiNghiepVu(MaLoi.GUI_LAI_QUA_NHANH);
        }
        // Sau khi giữ được khoá chờ (quy ước của TokenLienKetService); liên kết của yêu cầu trước hết hiệu lực
        String token = tokenLienKetService.tao(idTaiKhoan, MucDichLienKet.DOI_EMAIL, lienKetProperties.thoiHanDoiEmail(),
                emailMoi);
        eventPublisher.publishEvent(new EmailXacNhanDoiEmailEvent(emailMoi, token));
        eventPublisher.publishEvent(new EmailYeuCauDoiEmailEvent(taiKhoan.getEmail(), taiKhoan.getHoTen(), emailMoi));
        log.info("Gửi liên kết xác nhận đổi email id={}", idTaiKhoan);
        return new YeuCauDoiEmailResponse(emailMoi);
    }

    @Override
    public YeuCauDoiEmailResponse layYeuCauDangCho(Long idTaiKhoan) {
        taiKhoanService.layDangHoatDong(idTaiKhoan);
        return tokenLienKetService.xemDuLieu(idTaiKhoan, MucDichLienKet.DOI_EMAIL)
                .map(YeuCauDoiEmailResponse::new)
                .orElse(null);
    }

    @Override
    public void huy(Long idTaiKhoan) {
        taiKhoanService.layDangHoatDong(idTaiKhoan);
        tokenLienKetService.huy(idTaiKhoan, MucDichLienKet.DOI_EMAIL);
        log.info("Huỷ yêu cầu đổi email id={}", idTaiKhoan);
    }

    @Override
    @Transactional
    public void xacNhan(String token) {
        // Mọi lỗi đều ném trước khi commit: token đã bị tiêu, email không đổi
        LienKetDaDung lienKet = tokenLienKetService.suDungKemDuLieu(token, MucDichLienKet.DOI_EMAIL);
        Long idTaiKhoan = lienKet.idTaiKhoan();
        String emailMoi = lienKet.duLieu();
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE));
        switch (taiKhoan.getTrangThai()) {
            case VO_HIEU_HOA -> throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
            // Chỉ tài khoản đã kích hoạt mới yêu cầu đổi email được, nên không thể xảy ra
            case CHO_XAC_NHAN -> throw new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE);
            case DA_KICH_HOAT -> {
                // Được đổi
            }
        }
        // Token DOI_EMAIL luôn kèm email mới (xem yeuCau)
        if (emailMoi == null) {
            throw new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE);
        }
        // Kiểm tra lại: từ lúc yêu cầu tới giờ có thể đã có tài khoản khác đăng ký email này
        if (taiKhoanRepository.existsByEmail(emailMoi)) {
            throw new LoiNghiepVu(MaLoi.EMAIL_DA_TON_TAI);
        }

        String emailCu = taiKhoan.getEmail();
        String hoTen = taiKhoan.getHoTen();
        taiKhoan.setEmail(emailMoi);
        try {
            // Flush ngay để 2 request cùng lấy 1 email (lọt qua existsByEmail) dừng ở uk_tai_khoan_email với mã lỗi đúng
            taiKhoanRepository.saveAndFlush(taiKhoan);
        } catch (DataIntegrityViolationException e) {
            throw new LoiNghiepVu(MaLoi.EMAIL_DA_TON_TAI);
        }
        // Liên kết được mở ngoài mọi phiên đăng nhập nên không giữ lại thiết bị nào
        int soPhien = refreshTokenService.thuHoiTatCaCuaTaiKhoan(idTaiKhoan);
        // Liên kết đặt lại mật khẩu đang chờ đã được gửi tới email cũ
        tokenLienKetService.huy(idTaiKhoan, MucDichLienKet.DAT_LAI_MAT_KHAU);
        // Ai đó từng thử đăng nhập sai bằng email mới (khi nó chưa thuộc tài khoản nào) không được khoá chủ tài khoản
        gioiHanDangNhapService.xoa(emailMoi);
        eventPublisher.publishEvent(new EmailDaDoiEmailEvent(emailCu, hoTen, emailMoi));
        log.info("Đổi email đăng nhập id={}, thu hồi {} phiên", idTaiKhoan, soPhien);
    }

}
