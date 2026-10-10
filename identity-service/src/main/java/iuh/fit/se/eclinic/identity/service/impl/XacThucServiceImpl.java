package iuh.fit.se.eclinic.identity.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.LienKetProperties;
import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanResponse;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.mapper.TaiKhoanMapper;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;
import iuh.fit.se.eclinic.identity.service.XacThucService;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class XacThucServiceImpl implements XacThucService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final TokenLienKetService tokenLienKetService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final LienKetProperties lienKetProperties;
    private final TaiKhoanMapper taiKhoanMapper;

    @Override
    @Transactional
    public TaiKhoanResponse dangKy(DangKyRequest request) {
        String email = ChuanHoa.email(request.email());
        String soDienThoai = request.soDienThoai().trim();

        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(email).orElse(null);
        if (taiKhoan == null) {
            if (taiKhoanRepository.existsBySoDienThoai(soDienThoai)) {
                throw new LoiNghiepVu(MaLoi.SO_DIEN_THOAI_DA_TON_TAI);
            }
            taiKhoan = new TaiKhoan();
            taiKhoan.setEmail(email);
            taiKhoan.setVaiTro(VaiTro.BENH_NHAN);
            taiKhoan.setTrangThai(TrangThaiTaiKhoan.CHO_XAC_NHAN);
            ganThongTin(taiKhoan, request, soDienThoai);
            taiKhoan = taiKhoanRepository.save(taiKhoan);
            // id mới (MySQL không dùng lại id) nên chắc chắn giữ được khoá chờ; vẫn phải gọi để chặn gửi lại ngay
            tokenLienKetService.giuCho(taiKhoan.getId(), MucDichLienKet.XAC_THUC_EMAIL,
                    lienKetProperties.thoiGianChoGuiLai());
            log.info("Đăng ký tài khoản mới id={}", taiKhoan.getId());
        } else {
            if (taiKhoan.getTrangThai() != TrangThaiTaiKhoan.CHO_XAC_NHAN) {
                throw new LoiNghiepVu(MaLoi.EMAIL_DA_TON_TAI);
            }
            // Kiểm tra TRƯỚC khi sửa entity (xem ChuyenKhoaServiceImpl.capNhat) và trước khi giữ khoá chờ
            if (taiKhoanRepository.existsBySoDienThoaiAndIdNot(soDienThoai, taiKhoan.getId())) {
                throw new LoiNghiepVu(MaLoi.SO_DIEN_THOAI_DA_TON_TAI);
            }
            if (!tokenLienKetService.giuCho(taiKhoan.getId(), MucDichLienKet.XAC_THUC_EMAIL,
                    lienKetProperties.thoiGianChoGuiLai())) {
                throw new LoiNghiepVu(MaLoi.GUI_LAI_QUA_NHANH);
            }
            // Ghi đè: người đăng ký trước (có thể không phải chủ email) mất quyền, liên kết cũ hết hiệu lực
            ganThongTin(taiKhoan, request, soDienThoai);
            log.info("Đăng ký lại tài khoản đang chờ kích hoạt id={}", taiKhoan.getId());
        }

        guiLienKetXacThuc(taiKhoan);
        return taiKhoanMapper.toResponse(taiKhoan);
    }

    @Override
    @Transactional
    public void xacThucEmail(String token) {
        Long idTaiKhoan = tokenLienKetService.suDung(token, MucDichLienKet.XAC_THUC_EMAIL);
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE));
        switch (taiKhoan.getTrangThai()) {
            case VO_HIEU_HOA -> throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
            case CHO_XAC_NHAN -> {
                taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
                log.info("Kích hoạt tài khoản id={}", idTaiKhoan);
            }
            case DA_KICH_HOAT -> {
                // Đã kích hoạt: coi như thành công
            }
        }
    }

    @Override
    @Transactional
    public void guiLaiXacThuc(String email) {
        taiKhoanRepository.findByEmail(ChuanHoa.email(email))
                .filter(tk -> tk.getTrangThai() == TrangThaiTaiKhoan.CHO_XAC_NHAN)
                .filter(tk -> tokenLienKetService.giuCho(tk.getId(), MucDichLienKet.XAC_THUC_EMAIL,
                        lienKetProperties.thoiGianChoGuiLai()))
                .ifPresent(tk -> {
                    guiLienKetXacThuc(tk);
                    log.info("Gửi lại liên kết kích hoạt id={}", tk.getId());
                });
    }

    /** Gọi sau khi đã giữ khoá chờ (quy ước của TokenLienKetService). Email đi sau khi transaction commit. */
    private void guiLienKetXacThuc(TaiKhoan taiKhoan) {
        String token = tokenLienKetService.tao(taiKhoan.getId(), MucDichLienKet.XAC_THUC_EMAIL,
                lienKetProperties.thoiHanXacThucEmail());
        eventPublisher.publishEvent(new EmailXacThucEvent(taiKhoan.getEmail(), taiKhoan.getHoTen(), token));
    }

    private void ganThongTin(TaiKhoan taiKhoan, DangKyRequest request, String soDienThoai) {
        taiKhoan.setHoTen(request.hoTen().trim());
        taiKhoan.setSoDienThoai(soDienThoai);
        // Chỉ lưu lời khai; booking-service liên kết hồ sơ bệnh nhân theo số này khi bệnh nhân đăng nhập (quy tắc #3)
        taiKhoan.setCccdDangKy(request.cccd() == null || request.cccd().isBlank() ? null : request.cccd());
        taiKhoan.setMatKhauHash(passwordEncoder.encode(request.matKhau()));
    }

}
