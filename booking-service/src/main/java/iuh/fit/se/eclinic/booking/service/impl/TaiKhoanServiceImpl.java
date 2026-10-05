package iuh.fit.se.eclinic.booking.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.TaiKhoanChiDocRepository;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
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
    private final BacSiChiDocRepository bacSiChiDocRepository;

    @Override
    public TaiKhoan layBenhNhanDangHoatDong(Long id) {
        return layDangHoatDong(id, VaiTro.BENH_NHAN);
    }

    @Override
    public BacSi layBacSiDangHoatDong(Long idTaiKhoan) {
        layDangHoatDong(idTaiKhoan, VaiTro.BAC_SI);
        return bacSiChiDocRepository.findByTaiKhoanId(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN));
    }

    private TaiKhoan layDangHoatDong(Long id, VaiTro vaiTro) {
        TaiKhoan taiKhoan = taiKhoanChiDocRepository.findById(id)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.CHUA_DANG_NHAP));
        if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.VO_HIEU_HOA) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        }
        if (taiKhoan.getTrangThai() != TrangThaiTaiKhoan.DA_KICH_HOAT) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_CHUA_XAC_THUC);
        }
        if (taiKhoan.getVaiTro() != vaiTro) {
            throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN);
        }
        return taiKhoan;
    }

}
