package iuh.fit.se.eclinic.identity.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.TaiKhoanService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaiKhoanServiceImpl implements TaiKhoanService {

    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    public TaiKhoan layTheoId(Long id) {
        return taiKhoanRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("TaiKhoan", id));
    }

    @Override
    public TaiKhoan layDangHoatDong(Long id) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(id).orElseThrow(() -> new LoiNghiepVu(MaLoi.CHUA_DANG_NHAP));
        if (taiKhoan.getTrangThai() == TrangThaiTaiKhoan.VO_HIEU_HOA) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        }
        if (taiKhoan.getTrangThai() != TrangThaiTaiKhoan.DA_KICH_HOAT) {
            throw new LoiNghiepVu(MaLoi.TAI_KHOAN_CHUA_XAC_THUC);
        }
        return taiKhoan;
    }

    @Override
    @Transactional
    public String capNhatAnhDaiDien(Long id, String url) {
        TaiKhoan taiKhoan = layDangHoatDong(id);
        String anhCu = taiKhoan.getAnhDaiDien();
        taiKhoan.setAnhDaiDien(url);
        return anhCu;
    }

    @Override
    public Optional<TaiKhoan> timTheoEmail(String email) {
        return taiKhoanRepository.findByEmail(email);
    }

    @Override
    public boolean tonTaiEmail(String email) {
        return taiKhoanRepository.existsByEmail(email);
    }

}
