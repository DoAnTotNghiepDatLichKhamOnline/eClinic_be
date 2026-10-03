package iuh.fit.se.eclinic.identity.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatHoSoRequest;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.mapper.HoSoCaNhanMapper;
import iuh.fit.se.eclinic.identity.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.identity.repository.HoSoBenhNhanChiDocRepository;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.HoSoCaNhanService;
import iuh.fit.se.eclinic.identity.service.TaiKhoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoSoCaNhanServiceImpl implements HoSoCaNhanService {

    private final TaiKhoanService taiKhoanService;
    private final TaiKhoanRepository taiKhoanRepository;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final HoSoBenhNhanChiDocRepository hoSoBenhNhanChiDocRepository;
    private final HoSoCaNhanMapper hoSoCaNhanMapper;

    @Override
    public HoSoCaNhanResponse layHoSo(Long idTaiKhoan) {
        return taoResponse(taiKhoanService.layDangHoatDong(idTaiKhoan));
    }

    @Override
    @Transactional
    public HoSoCaNhanResponse capNhat(Long idTaiKhoan, CapNhatHoSoRequest request) {
        TaiKhoan taiKhoan = taiKhoanService.layDangHoatDong(idTaiKhoan);
        String hoTen = request.hoTen().trim();
        String soDienThoai = request.soDienThoai();

        // Tên bác sĩ hiển thị cho bệnh nhân khi đặt lịch và trên hồ sơ bệnh án: chỉ quản trị viên được đổi (DOC-01)
        if (taiKhoan.getVaiTro() == VaiTro.BAC_SI && !hoTen.equals(taiKhoan.getHoTen())) {
            throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN,
                    "Họ tên bác sĩ do quản trị viên quản lý, bạn chỉ có thể đổi số điện thoại");
        }
        // Kiểm tra trùng TRƯỚC khi sửa entity (xem ChuyenKhoaServiceImpl.capNhat)
        if (soDienThoai != null && taiKhoanRepository.existsBySoDienThoaiAndIdNot(soDienThoai, idTaiKhoan)) {
            throw new LoiNghiepVu(MaLoi.SO_DIEN_THOAI_DA_TON_TAI);
        }

        taiKhoan.setHoTen(hoTen);
        if (soDienThoai != null) {
            taiKhoan.setSoDienThoai(soDienThoai);
        }
        log.info("Cập nhật hồ sơ cá nhân tài khoản id={}", idTaiKhoan);
        return taoResponse(taiKhoan);
    }

    /** Hồ sơ theo vai trò: quản trị viên không có hồ sơ riêng nên không truy vấn gì thêm. */
    private HoSoCaNhanResponse taoResponse(TaiKhoan taiKhoan) {
        BacSi bacSi = taiKhoan.getVaiTro() == VaiTro.BAC_SI
                ? bacSiChiDocRepository.findByTaiKhoanId(taiKhoan.getId()).orElse(null)
                : null;
        HoSoBenhNhan hoSoBenhNhan = taiKhoan.getVaiTro() == VaiTro.BENH_NHAN
                ? hoSoBenhNhanChiDocRepository.findByTaiKhoanId(taiKhoan.getId()).orElse(null)
                : null;
        return hoSoCaNhanMapper.toResponse(taiKhoan, bacSi, hoSoBenhNhan);
    }

}
