package iuh.fit.se.eclinic.medical.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.medical.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.medical.repository.TaiKhoanChiDocRepository;
import iuh.fit.se.eclinic.medical.service.BacSiDangNhapService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BacSiDangNhapServiceImpl implements BacSiDangNhapService {

    private final TaiKhoanChiDocRepository taiKhoanChiDocRepository;
    private final BacSiChiDocRepository bacSiChiDocRepository;

    @Override
    public BacSi layBacSiDangHoatDong(Long idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanChiDocRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.CHUA_DANG_NHAP));
        if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.VO_HIEU_HOA) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        }
        if (taiKhoan.getTrangThai() != TrangThaiTaiKhoan.DA_KICH_HOAT) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_CHUA_XAC_THUC);
        }
        if (taiKhoan.getVaiTro() != VaiTro.BAC_SI) {
            throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN);
        }
        return bacSiChiDocRepository.findByTaiKhoanId(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN));
    }

}
