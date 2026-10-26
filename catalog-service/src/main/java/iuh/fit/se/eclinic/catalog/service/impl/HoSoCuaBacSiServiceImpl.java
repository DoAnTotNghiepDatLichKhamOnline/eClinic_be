package iuh.fit.se.eclinic.catalog.service.impl;

import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoCuaToiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.service.AnhBacSiService;
import iuh.fit.se.eclinic.catalog.service.HoSoBacSiService;
import iuh.fit.se.eclinic.catalog.service.HoSoCuaBacSiService;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

/**
 * Tìm bác sĩ của tài khoản đang đăng nhập rồi chuyển cho {@link HoSoBacSiService} / {@link AnhBacSiService}.
 * <p>
 * CỐ Ý không có @Transactional ở class: thêm ảnh gọi kho ảnh (tới 20 giây), không được giữ transaction trong lúc đó
 * (xem AnhBacSiServiceImpl). Mỗi service được gọi tự mở transaction của mình.
 */
@Service
@RequiredArgsConstructor
public class HoSoCuaBacSiServiceImpl implements HoSoCuaBacSiService {

    private final BacSiRepository bacSiRepository;
    private final HoSoBacSiService hoSoBacSiService;
    private final AnhBacSiService anhBacSiService;

    @Override
    public HoSoBacSiQuanTriResponse layHoSo(Long idTaiKhoan) {
        return hoSoBacSiService.layHoSo(idBacSi(idTaiKhoan));
    }

    @Override
    public HoSoBacSiQuanTriResponse capNhatHoSo(Long idTaiKhoan, CapNhatHoSoCuaToiRequest request) {
        return hoSoBacSiService.capNhatHoSoCuaToi(idBacSi(idTaiKhoan), request);
    }

    @Override
    public AnhBacSiQuanTriResponse themAnh(Long idTaiKhoan, String chuThich, String chuThichEn, byte[] noiDung) {
        return anhBacSiService.them(idBacSi(idTaiKhoan), LoaiAnhBacSi.ANH_CONG_VIEC, chuThich, chuThichEn, noiDung);
    }

    @Override
    public AnhBacSiQuanTriResponse capNhatAnh(Long idTaiKhoan, Long idAnh, String chuThich, String chuThichEn) {
        return anhBacSiService.capNhatChuThichAnhCongViec(idBacSi(idTaiKhoan), idAnh, chuThich, chuThichEn);
    }

    @Override
    public void xoaAnh(Long idTaiKhoan, Long idAnh) {
        anhBacSiService.xoaAnhCongViec(idBacSi(idTaiKhoan), idAnh);
    }

    /** JWT còn hạn sau khi tài khoản bị vô hiệu hoá (vd bác sĩ ngừng công tác): kiểm tra lại ở đây. */
    private Long idBacSi(Long idTaiKhoan) {
        BacSi bacSi = bacSiRepository.timTheoTaiKhoanKemTaiKhoan(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN));
        TaiKhoan taiKhoan = bacSi.getTaiKhoan();
        if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.VO_HIEU_HOA) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        }
        return bacSi.getId();
    }

}
