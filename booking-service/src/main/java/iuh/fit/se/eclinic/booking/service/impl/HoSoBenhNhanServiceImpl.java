package iuh.fit.se.eclinic.booking.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.HoSoCuaToiRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoCuaToiResponse;
import iuh.fit.se.eclinic.booking.mapper.HoSoBenhNhanMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.service.HoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.service.LienKetHoSoService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.util.ChuanHoaTen;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoSoBenhNhanServiceImpl implements HoSoBenhNhanService {

    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final TaiKhoanService taiKhoanService;
    private final HoSoBenhNhanMapper hoSoBenhNhanMapper;
    private final LienKetHoSoService lienKetHoSoService;

    @Override
    public HoSoBenhNhan layTheoId(Long id) {
        return hoSoBenhNhanRepository.findById(id)
                .orElseThrow(() -> new LoiKhongTimThay("HoSoBenhNhan", id));
    }

    @Override
    public Optional<HoSoBenhNhan> timTheoTaiKhoanId(Long taiKhoanId) {
        return hoSoBenhNhanRepository.findByTaiKhoanId(taiKhoanId);
    }

    @Override
    public Optional<HoSoBenhNhan> timTheoCccd(String cccd) {
        return hoSoBenhNhanRepository.findByCccd(cccd);
    }

    @Override
    public Optional<HoSoBenhNhan> timDaLienKetCuaTaiKhoan(Long idTaiKhoan) {
        return hoSoBenhNhanRepository.findByTaiKhoanId(idTaiKhoan)
                .filter(hoSo -> hoSo.getTrangThaiLienKet() == TrangThaiLienKet.DA_LIEN_KET);
    }

    @Override
    public HoSoCuaToiResponse xemCuaToi(Long idTaiKhoan) {
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        HoSoBenhNhan hoSo = hoSoBenhNhanRepository.findByTaiKhoanId(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Tài khoản chưa có hồ sơ bệnh nhân"));
        return hoSoBenhNhanMapper.toHoSoCuaToi(hoSo);
    }

    @Override
    @Transactional
    public HoSoCuaToiResponse luuCuaToi(Long idTaiKhoan, HoSoCuaToiRequest request) {
        TaiKhoan taiKhoan = taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        String cccd = rongThanhNull(request.cccd());
        Optional<HoSoBenhNhan> daCo = hoSoBenhNhanRepository.findByTaiKhoanId(idTaiKhoan);
        HoSoBenhNhan hoSo;
        if (daCo.isEmpty()) {
            if (cccd == null) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Số CCCD không được để trống khi tạo hồ sơ bệnh nhân");
            }
            Optional<HoSoBenhNhan> theoCccd = hoSoBenhNhanRepository.findByCccdForUpdate(cccd);
            if (theoCccd.isPresent()) {
                // Gắn hồ sơ đã có (tạo khi đặt lịch như khách) vào tài khoản phải qua xác minh (quy tắc #3)
                hoSo = theoCccd.get();
                if (hoSo.getTaiKhoan() != null || lienKetHoSoService.daBiTuChoi(idTaiKhoan, hoSo.getId())) {
                    throw new LoiNghiepVu(MaLoi.CCCD_DA_CO_HO_SO);
                }
                boolean khop = ChuanHoaTen.giongNhau(request.hoTen(), hoSo.getHoTen())
                        && (request.ngaySinh().equals(hoSo.getNgaySinh())
                                || request.soDienThoai().equals(hoSo.getSoDienThoai()));
                lienKetHoSoService.lienKet(taiKhoan, hoSo, khop);
                if (!khop) {
                    // Chờ quản trị viên xác minh: không ghi thông tin trong form lên hồ sơ, chỉ trả trạng thái
                    return hoSoBenhNhanMapper.toHoSoCuaToi(hoSo);
                }
            } else {
                hoSo = new HoSoBenhNhan();
                hoSo.setCccd(cccd);
                hoSo.setTaiKhoan(taiKhoan);
                hoSo.setTrangThaiLienKet(TrangThaiLienKet.DA_LIEN_KET);
            }
        } else {
            hoSo = daCo.get();
            if (hoSo.getTrangThaiLienKet() != TrangThaiLienKet.DA_LIEN_KET) {
                throw new LoiNghiepVu(MaLoi.HO_SO_CHO_XAC_MINH);
            }
            if (cccd != null && !cccd.equals(hoSo.getCccd())) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Không thể đổi số CCCD của hồ sơ bệnh nhân");
            }
        }
        hoSo.setHoTen(ChuanHoaTen.gon(request.hoTen()));
        hoSo.setNgaySinh(request.ngaySinh());
        hoSo.setGioiTinh(request.gioiTinh());
        hoSo.setSoDienThoai(request.soDienThoai());
        hoSo.setDiaChi(rongThanhNull(request.diaChi()));
        hoSo.setSoBaoHiemYTe(rongThanhNull(request.soBaoHiemYTe()));
        hoSo.setTienSuBenhLy(rongThanhNull(request.tienSuBenhLy()));
        // Flush ngay: 2 request tạo hồ sơ cùng lúc thì request sau vi phạm UNIQUE cccd / id_tai_khoan -> 409 XUNG_DOT_DU_LIEU
        return hoSoBenhNhanMapper.toHoSoCuaToi(hoSoBenhNhanRepository.saveAndFlush(hoSo));
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }

}
