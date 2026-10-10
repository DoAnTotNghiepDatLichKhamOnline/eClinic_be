package iuh.fit.se.eclinic.identity.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.event.EmailKichHoatLaiTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailVoHieuHoaTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;
import iuh.fit.se.eclinic.identity.service.TrangThaiTaiKhoanService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class TrangThaiTaiKhoanServiceImpl implements TrangThaiTaiKhoanService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenLienKetService tokenLienKetService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public int voHieuHoa(TaiKhoan taiKhoan, String lyDo) {
        Long id = taiKhoan.getId();
        String email = taiKhoan.getEmail();
        String hoTen = taiKhoan.getHoTen();
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoan.setLyDoVoHieuHoa(lyDo);
        // UPDATE thu hồi flush trước rồi mới clear persistence context, nên trạng thái mới không bị mất
        int soPhien = refreshTokenService.thuHoiTatCaCuaTaiKhoan(id);
        // Liên kết đã gửi trước khi vô hiệu hoá không được dùng lại sau khi tài khoản được kích hoạt lại
        huyMoiLienKet(id);
        eventPublisher.publishEvent(new EmailVoHieuHoaTaiKhoanEvent(email, hoTen, lyDo));
        return soPhien;
    }

    @Override
    public void kichHoatLai(TaiKhoan taiKhoan) {
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
        taiKhoan.setLyDoVoHieuHoa(null);
        // Flush ngay để ngayCapNhat trong response là giá trị mới
        taiKhoanRepository.flush();
        eventPublisher.publishEvent(new EmailKichHoatLaiTaiKhoanEvent(taiKhoan.getEmail(), taiKhoan.getHoTen()));
    }

    @Override
    public void huyMoiLienKet(Long idTaiKhoan) {
        for (MucDichLienKet mucDich : MucDichLienKet.values()) {
            tokenLienKetService.huy(idTaiKhoan, mucDich);
        }
    }

}
