package iuh.fit.se.eclinic.booking.service.impl;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.PhieuKhamProperties;
import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.PhieuKhamService;
import iuh.fit.se.eclinic.booking.util.MaQr;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhieuKhamServiceImpl implements PhieuKhamService {

    /** Dạng của TokenNgauNhien.tao(): 256 bit, base64url không đệm = 43 ký tự. Id số hay chuỗi khác không chạm tới DB. */
    private static final Pattern DANG_MA_PHIEU_KHAM = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final LichHenRepository lichHenRepository;
    private final LichHenMapper lichHenMapper;
    private final PhieuKhamProperties phieuKhamProperties;

    @Override
    public PhieuKhamResponse xem(String maPhieuKham) {
        kiemTraDang(maPhieuKham);
        return lichHenRepository.timTheoMaPhieuKham(maPhieuKham)
                // Collation của cột không phân biệt hoa thường; mã phải khớp từng ký tự
                .filter(lichHen -> lichHen.getMaTokenPhieuKham().equals(maPhieuKham))
                .map(lichHenMapper::toPhieuKhamResponse)
                .orElseThrow(PhieuKhamServiceImpl::khongTimThay);
    }

    @Override
    public byte[] taoQr(String maPhieuKham, Integer kichThuoc) {
        kiemTraDang(maPhieuKham);
        lichHenRepository.timMaPhieuKham(maPhieuKham).filter(maPhieuKham::equals)
                .orElseThrow(PhieuKhamServiceImpl::khongTimThay);
        return MaQr.taoPng(phieuKhamProperties.lienKet(maPhieuKham),
                kichThuoc != null ? kichThuoc : phieuKhamProperties.kichThuocQr());
    }

    private static void kiemTraDang(String maPhieuKham) {
        if (maPhieuKham == null || !DANG_MA_PHIEU_KHAM.matcher(maPhieuKham).matches()) {
            throw khongTimThay();
        }
    }

    /** Một thông điệp chung cho mọi trường hợp, không cho biết mã sai dạng hay không tồn tại. */
    private static LoiNghiepVu khongTimThay() {
        return new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Phiếu khám không tồn tại");
    }

}
