package iuh.fit.se.eclinic.medical.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.LichSuSuaBenhAn;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.medical.dto.response.LichSuSuaBenhAnResponse;
import iuh.fit.se.eclinic.medical.dto.response.NoiDungBenhAn;
import iuh.fit.se.eclinic.medical.repository.HoSoBenhAnRepository;
import iuh.fit.se.eclinic.medical.repository.LichHenKhamRepository;
import iuh.fit.se.eclinic.medical.repository.LichSuSuaBenhAnRepository;
import iuh.fit.se.eclinic.medical.service.BacSiDangNhapService;
import iuh.fit.se.eclinic.medical.service.LichSuSuaBenhAnService;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichSuSuaBenhAnServiceImpl implements LichSuSuaBenhAnService {

    private final BacSiDangNhapService bacSiDangNhapService;
    private final LichHenKhamRepository lichHenKhamRepository;
    private final HoSoBenhAnRepository hoSoBenhAnRepository;
    private final LichSuSuaBenhAnRepository lichSuSuaBenhAnRepository;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public void ghi(HoSoBenhAn benhAn, BacSi bacSi, String lyDo, NoiDungBenhAn truoc, NoiDungBenhAn sau) {
        LichSuSuaBenhAn lanSua = new LichSuSuaBenhAn();
        lanSua.setHoSoBenhAn(benhAn);
        lanSua.setPhienBan(benhAn.getPhienBan());
        lanSua.setBacSi(bacSi);
        lanSua.setLyDo(lyDo);
        lanSua.setNoiDungTruoc(jsonMapper.writeValueAsString(truoc));
        lanSua.setNoiDungSau(jsonMapper.writeValueAsString(sau));
        lichSuSuaBenhAnRepository.save(lanSua);
    }

    @Override
    public LichSuSuaBenhAnResponse cuaBacSi(Long idTaiKhoanBacSi, Long idLichHen) {
        BacSi bacSi = bacSiDangNhapService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        // Không được xem và không có lịch hẹn trả cùng 1 lỗi: không lộ lượt khám nào tồn tại
        LichHen lichHen = lichHenKhamRepository.findById(idLichHen)
                .filter(timThay -> lichHenKhamRepository.bacSiXemDuocLuotKham(bacSi.getId(),
                        timThay.getHoSoBenhNhan().getId(), timThay.getKhungGio().getGioBatDau()))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.lichHen"));
        return lichSu(lichHen);
    }

    @Override
    public LichSuSuaBenhAnResponse choQuanTri(Long idLichHen) {
        LichHen lichHen = lichHenKhamRepository.findById(idLichHen)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.lichHen"));
        return lichSu(lichHen);
    }

    private LichSuSuaBenhAnResponse lichSu(LichHen lichHen) {
        HoSoBenhAn benhAn = hoSoBenhAnRepository.findByLichHenId(lichHen.getId())
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.benhAn"));
        List<LichSuSuaBenhAnResponse.LanSua> cacLanSua = lichSuSuaBenhAnRepository.timCuaHoSoBenhAn(benhAn.getId())
                .stream()
                .map(lanSua -> {
                    NoiDungBenhAn truoc = jsonMapper.readValue(lanSua.getNoiDungTruoc(), NoiDungBenhAn.class);
                    NoiDungBenhAn sau = jsonMapper.readValue(lanSua.getNoiDungSau(), NoiDungBenhAn.class);
                    BacSi bacSi = lanSua.getBacSi();
                    return new LichSuSuaBenhAnResponse.LanSua(lanSua.getPhienBan(), lanSua.getNgayTao(),
                            new LichSuSuaBenhAnResponse.BacSiSua(bacSi.getId(), bacSi.getTaiKhoan().getHoTen()),
                            lanSua.getLyDo(), NoiDungBenhAn.cacTruongDoi(truoc, sau), truoc, sau);
                })
                .toList();
        return new LichSuSuaBenhAnResponse(lichHen.getId(), lichHen.getMaTraCuu(), benhAn.getPhienBan(),
                benhAn.getNgayTao(), cacLanSua);
    }

}
