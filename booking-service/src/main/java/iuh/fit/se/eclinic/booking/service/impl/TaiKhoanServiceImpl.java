package iuh.fit.se.eclinic.booking.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.TaiKhoanChiDocRepository;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaiKhoanServiceImpl implements TaiKhoanService {

    private final TaiKhoanChiDocRepository taiKhoanChiDocRepository;

    @Override
    public TaiKhoan layBenhNhanDangHoatDong(Long id) {
        TaiKhoan taiKhoan = taiKhoanChiDocRepository.findById(id)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.CHUA_DANG_NHAP));
        if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.VO_HIEU_HOA) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        }
        if (taiKhoan.getTrangThai() != TrangThaiTaiKhoan.DA_KICH_HOAT) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_CHUA_XAC_THUC);
        }
        if (taiKhoan.getVaiTro() != VaiTro.BENH_NHAN) {
            throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN);
        }
        return taiKhoan;
    }

}
