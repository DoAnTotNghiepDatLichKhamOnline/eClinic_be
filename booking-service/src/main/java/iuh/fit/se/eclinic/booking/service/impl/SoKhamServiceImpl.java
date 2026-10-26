package iuh.fit.se.eclinic.booking.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.response.SoKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.SoKhamTomTatResponse;
import iuh.fit.se.eclinic.booking.mapper.SoKhamMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhAnChiDocRepository;
import iuh.fit.se.eclinic.booking.service.GioiHanXemSoKhamService;
import iuh.fit.se.eclinic.booking.service.SoKhamService;
import iuh.fit.se.eclinic.booking.util.MaQr;
import iuh.fit.se.eclinic.common.config.SoKhamProperties;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SoKhamServiceImpl implements SoKhamService {

    /** Dạng của TokenNgauNhien.tao(): 256 bit, base64url không đệm = 43 ký tự. Chuỗi khác không chạm tới DB. */
    private static final Pattern DANG_MA_SO_KHAM = Pattern.compile("[A-Za-z0-9_-]{43}");

    private static final int SO_KY_TU_DOI_CHIEU = 4;

    private final HoSoBenhAnChiDocRepository hoSoBenhAnChiDocRepository;
    private final SoKhamMapper soKhamMapper;
    private final SoKhamProperties soKhamProperties;
    private final GioiHanXemSoKhamService gioiHanXemSoKhamService;

    @Override
    public SoKhamTomTatResponse tomTat(String maSoKham) {
        return soKhamMapper.toTomTat(tim(maSoKham));
    }

    @Override
    public SoKhamResponse xem(String maSoKham, String bonSoCuoi) {
        HoSoBenhAn benhAn = tim(maSoKham);
        gioiHanXemSoKhamService.kiemTraKhoa(benhAn.getId());
        if (!khopBonSoCuoi(benhAn.getLichHen(), bonSoCuoi)) {
            gioiHanXemSoKhamService.ghiNhanSai(benhAn.getId());
            throw new LoiNghiepVu(MaLoi.SO_DIEN_THOAI_KHONG_KHOP, "loi.SO_DIEN_THOAI_KHONG_KHOP.soKham");
        }
        gioiHanXemSoKhamService.xoa(benhAn.getId());
        // Tải đơn thuốc, thuốc, ICD-10 vào chính hồ sơ đang giữ (cùng persistence context)
        hoSoBenhAnChiDocRepository.timTheoLichHenKemDonThuoc(List.of(benhAn.getLichHen().getId()));
        return soKhamMapper.toResponse(benhAn);
    }

    @Override
    public byte[] taoQr(String maSoKham, Integer kichThuoc) {
        kiemTraDang(maSoKham);
        hoSoBenhAnChiDocRepository.timMaSoKham(maSoKham).filter(maSoKham::equals)
                .orElseThrow(SoKhamServiceImpl::khongTimThay);
        return MaQr.taoPng(soKhamProperties.lienKet(maSoKham),
                kichThuoc != null ? kichThuoc : soKhamProperties.kichThuocQr());
    }

    private HoSoBenhAn tim(String maSoKham) {
        kiemTraDang(maSoKham);
        return hoSoBenhAnChiDocRepository.timTheoMaSoKham(maSoKham)
                // Collation của cột không phân biệt hoa thường; mã phải khớp từng ký tự
                .filter(benhAn -> benhAn.getMaTokenSoKham().equals(maSoKham))
                .orElseThrow(SoKhamServiceImpl::khongTimThay);
    }

    /** SĐT liên hệ của lượt khám (lịch cũ: SĐT người giám hộ, rồi SĐT của hồ sơ). */
    private static boolean khopBonSoCuoi(LichHen lichHen, String bonSoCuoi) {
        String daLuu = lichHen.getSoDienThoaiLienHe();
        if (daLuu == null && lichHen.getNguoiGiamHo() != null) {
            daLuu = lichHen.getNguoiGiamHo().getSoDienThoai();
        }
        if (daLuu == null) {
            daLuu = lichHen.getHoSoBenhNhan().getSoDienThoai();
        }
        if (daLuu == null || daLuu.length() < SO_KY_TU_DOI_CHIEU || bonSoCuoi == null) {
            return false;
        }
        return MessageDigest.isEqual(daLuu.substring(daLuu.length() - SO_KY_TU_DOI_CHIEU).getBytes(StandardCharsets.UTF_8),
                bonSoCuoi.getBytes(StandardCharsets.UTF_8));
    }

    private static void kiemTraDang(String maSoKham) {
        if (maSoKham == null || !DANG_MA_SO_KHAM.matcher(maSoKham).matches()) {
            throw khongTimThay();
        }
    }

    /** Một thông điệp chung cho mọi trường hợp, không cho biết mã sai dạng hay không tồn tại. */
    private static LoiNghiepVu khongTimThay() {
        return new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.soKham");
    }

}
