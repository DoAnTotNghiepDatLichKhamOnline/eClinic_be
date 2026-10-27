package iuh.fit.se.eclinic.booking.mapper;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.LichHenQuanTriChiTietResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenQuanTriChiTietResponse.MatXich;
import iuh.fit.se.eclinic.booking.dto.response.LichHenQuanTriChiTietResponse.NguoiLam;
import iuh.fit.se.eclinic.booking.dto.response.LichHenQuanTriChiTietResponse.SuKien;
import iuh.fit.se.eclinic.booking.dto.response.LichHenQuanTriChiTietResponse.TaiKhoanDat;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.LichSuLichHen;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.thongbao.VanBanHeThong;
import lombok.RequiredArgsConstructor;

/** Chi tiết lịch hẹn của quản trị viên. */
@Component
@RequiredArgsConstructor
public class LichSuLichHenMapper {

    private final LichHenMapper lichHenMapper;
    private final MessageSource messageSource;

    public LichHenQuanTriChiTietResponse toChiTiet(LichHen lichHen, boolean quanTriHuyDoiDuoc, List<LichHen> chuoi,
            List<LichSuLichHen> lichSu) {
        TaiKhoan taiKhoanDat = lichHen.getTaiKhoanDat();
        NgonNgu ngonNgu = NgonNgu.hienTai();
        return new LichHenQuanTriChiTietResponse(lichHenMapper.toTrongCaResponse(lichHen, true),
                lichHen.getEmailLienHe(),
                taiKhoanDat == null ? null
                        : new TaiKhoanDat(taiKhoanDat.getId(), taiKhoanDat.getHoTen(), taiKhoanDat.getEmail()),
                lichHen.getNgonNgu(), quanTriHuyDoiDuoc,
                chuoi.stream().map(mat -> new MatXich(mat.getId(), mat.getMaTraCuu(), mat.getTrangThai(),
                        mat.getKhungGio().getGioBatDau(), mat.getId().equals(lichHen.getId()))).toList(),
                lichSu.stream().map(dong -> toSuKien(dong, ngonNgu)).toList());
    }

    private SuKien toSuKien(LichSuLichHen dong, NgonNgu ngonNgu) {
        TaiKhoan taiKhoan = dong.getTaiKhoanThucHien();
        LichHen lienQuan = dong.getLichHenLienQuan();
        return new SuKien(dong.getNgayTao(), dong.getLichHen().getMaTraCuu(), dong.getSuKien(),
                new NguoiLam(dong.getLoaiNguoiThucHien(), taiKhoan == null ? null : taiKhoan.getId(),
                        taiKhoan == null ? null : taiKhoan.getHoTen()),
                VanBanHeThong.doc(dong.getLyDo(), messageSource, ngonNgu),
                lienQuan == null ? null : lienQuan.getMaTraCuu());
    }

}
