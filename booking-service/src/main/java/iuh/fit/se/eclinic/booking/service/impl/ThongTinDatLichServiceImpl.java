package iuh.fit.se.eclinic.booking.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.response.HoSoCuaToiResponse;
import iuh.fit.se.eclinic.booking.dto.response.ThongTinDatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.ThongTinDatLichResponse.LanDatGanNhat;
import iuh.fit.se.eclinic.booking.mapper.HoSoBenhNhanMapper;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.NguoiThanDaLuuService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.service.ThongTinDatLichService;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThongTinDatLichServiceImpl implements ThongTinDatLichService {

    private final TaiKhoanService taiKhoanService;
    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final HoSoBenhNhanMapper hoSoBenhNhanMapper;
    private final NguoiThanDaLuuService nguoiThanDaLuuService;
    private final LichHenRepository lichHenRepository;

    @Override
    public ThongTinDatLichResponse cuaToi(Long idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        HoSoBenhNhan hoSo = hoSoBenhNhanRepository.findByTaiKhoanId(idTaiKhoan).orElse(null);
        HoSoCuaToiResponse banThan = hoSo == null ? null : hoSoBenhNhanMapper.toHoSoCuaToi(hoSo);
        boolean daLienKet = hoSo != null && hoSo.getTrangThaiLienKet() == TrangThaiLienKet.DA_LIEN_KET;
        // timCuaTaiKhoan: giờ khám muộn nhất trước, lấy sẵn bác sĩ + tài khoản + chuyên khoa
        LanDatGanNhat lanDatGanNhat = lichHenRepository
                .timCuaTaiKhoan(idTaiKhoan, daLienKet ? hoSo.getId() : null, daLienKet ? hoSo.getCccd() : null,
                        false, false, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(lichHen -> {
                    BacSi bacSi = lichHen.getBacSi();
                    return new LanDatGanNhat(bacSi.getChuyenKhoa().getId(), bacSi.getChuyenKhoa().getTenChuyenKhoa(),
                            bacSi.getId(), bacSi.getTaiKhoan().getHoTen());
                })
                .orElse(null);
        return new ThongTinDatLichResponse(banThan, taiKhoan.getEmail(),
                nguoiThanDaLuuService.cuaTaiKhoan(idTaiKhoan), lanDatGanNhat);
    }

}
