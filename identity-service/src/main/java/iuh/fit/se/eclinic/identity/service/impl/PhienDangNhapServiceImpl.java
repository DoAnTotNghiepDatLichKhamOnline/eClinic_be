package iuh.fit.se.eclinic.identity.service.impl;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.dto.response.PhienDangNhapResponse;
import iuh.fit.se.eclinic.identity.mapper.PhienDangNhapMapper;
import iuh.fit.se.eclinic.identity.service.PhienDangNhapService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.service.TaiKhoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhienDangNhapServiceImpl implements PhienDangNhapService {

    private final TaiKhoanService taiKhoanService;
    private final RefreshTokenService refreshTokenService;
    private final PhienDangNhapMapper phienDangNhapMapper;

    @Override
    public List<PhienDangNhapResponse> layDanhSach(Long idTaiKhoan, String maPhienHienTai) {
        taiKhoanService.layDangHoatDong(idTaiKhoan);
        // layPhienDangHoatDong đã xếp theo lần hoạt động gần nhất; sorted() ổn định nên chỉ đưa phiên hiện tại lên đầu
        return refreshTokenService.layPhienDangHoatDong(idTaiKhoan).stream()
                .map(phien -> phienDangNhapMapper.toResponse(phien, maPhienHienTai))
                .sorted(Comparator.comparing(PhienDangNhapResponse::hienTai).reversed())
                .toList();
    }

    @Override
    @Transactional
    public void dangXuat(Long idTaiKhoan, String maPhienHienTai, String maPhien) {
        taiKhoanService.layDangHoatDong(idTaiKhoan);
        if (maPhien.equals(maPhienHienTai)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "Đây là thiết bị bạn đang dùng, hãy dùng chức năng Đăng xuất");
        }
        // Lọc theo tài khoản: mã phiên của người khác cũng chỉ là "không tìm thấy"
        if (!refreshTokenService.thuHoiPhien(idTaiKhoan, maPhien)) {
            throw new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Phiên đăng nhập không tồn tại hoặc đã đăng xuất");
        }
        log.info("Đăng xuất từ xa phiên {} của tài khoản id={}", maPhien, idTaiKhoan);
    }

    @Override
    @Transactional
    public int dangXuatCacPhienKhac(Long idTaiKhoan, String maPhienHienTai) {
        taiKhoanService.layDangHoatDong(idTaiKhoan);
        int soPhien = refreshTokenService.thuHoiCacPhienKhac(idTaiKhoan, maPhienHienTai);
        log.info("Đăng xuất {} phiên khác của tài khoản id={}", soPhien, idTaiKhoan);
        return soPhien;
    }

}
