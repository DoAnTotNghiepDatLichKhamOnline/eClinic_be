package iuh.fit.se.eclinic.booking.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.LichSuCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichSuCaResponse.AnhCa;
import iuh.fit.se.eclinic.common.entity.scheduling.LichSuCaLamViec;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.thongbao.VanBanHeThong;
import lombok.RequiredArgsConstructor;

/** Chuyển lịch sử 1 ca sang DTO. */
@Component
@RequiredArgsConstructor
public class LichSuCaMapper {

    private final CaKhamMapper caKhamMapper;
    private final MessageSource messageSource;

    /** @param cacDong mọi dòng lịch sử của 1 ca, cũ nhất trước */
    public List<LichSuCaResponse> toResponses(List<LichSuCaLamViec> cacDong) {
        List<LichSuCaResponse> ketQua = new ArrayList<>();
        AnhCa truoc = null;
        for (LichSuCaLamViec dong : cacDong) {
            AnhCa sau = anhCa(dong);
            ketQua.add(new LichSuCaResponse(dong.getId(), dong.getHanhDong(), dong.getNguon(), dong.getNgayTao(),
                    new LichSuCaResponse.QuanTriThucHien(dong.getQuanTri().getId(),
                            dong.getQuanTri().getTaiKhoan().getHoTen()),
                    dong.getIdYeuCau(), VanBanHeThong.doc(dong.getLyDo(), messageSource, NgonNgu.hienTai()),
                    truoc, sau, cacTruongDoi(truoc, sau)));
            truoc = sau;
        }
        return ketQua;
    }

    private AnhCa anhCa(LichSuCaLamViec dong) {
        return new AnhCa(caKhamMapper.toBacSiTomTat(dong.getBacSi()),
                caKhamMapper.toPhongKhamTomTat(dong.getPhongKham()), dong.getNgayLamViec(), dong.getGioBatDau(), dong.getGioKetThuc(), dong.getSoLuotToiDaMoiGio(),
                dong.getThoiLuongLuotPhut());
    }

    private static List<String> cacTruongDoi(AnhCa truoc, AnhCa sau) {
        List<String> doi = new ArrayList<>();
        if (truoc == null) {
            return doi;
        }
        if (!Objects.equals(truoc.bacSi().id(), sau.bacSi().id())) {
            doi.add("bacSi");
        }
        if (!Objects.equals(truoc.phongKham().id(), sau.phongKham().id())) {
            doi.add("phongKham");
        }
        if (!Objects.equals(truoc.ngay(), sau.ngay())) {
            doi.add("ngay");
        }
        if (!Objects.equals(truoc.gioBatDau(), sau.gioBatDau())) {
            doi.add("gioBatDau");
        }
        if (!Objects.equals(truoc.gioKetThuc(), sau.gioKetThuc())) {
            doi.add("gioKetThuc");
        }
        if (!Objects.equals(truoc.soLuotToiDaMoiGio(), sau.soLuotToiDaMoiGio())) {
            doi.add("soLuotToiDaMoiGio");
        }
        if (!Objects.equals(truoc.thoiLuongLuotPhut(), sau.thoiLuongLuotPhut())) {
            doi.add("thoiLuongLuotPhut");
        }
        return doi;
    }

}
