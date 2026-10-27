package iuh.fit.se.eclinic.booking.mapper;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhamResponse;
import iuh.fit.se.eclinic.common.config.BenhAnProperties;
import iuh.fit.se.eclinic.common.config.SoKhamProperties;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.Icd10;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.RequiredArgsConstructor;

/** Chuyển hồ sơ bệnh án sang kết quả khám. */
@Component
@RequiredArgsConstructor
public class KetQuaKhamMapper {

    private final BenhAnProperties benhAnProperties;
    private final SoKhamProperties soKhamProperties;

    /** Cho bệnh nhân: thời điểm sửa hồ sơ, kèm mã và link sổ khám bệnh. */
    public KetQuaKhamResponse toResponse(HoSoBenhAn benhAn) {
        return toResponse(benhAn, false);
    }

    /** Cho bác sĩ: kèm phiên bản và hạn sửa; null nếu chưa có hồ sơ bệnh án. */
    public KetQuaKhamResponse toResponseChoBacSi(HoSoBenhAn benhAn) {
        return toResponse(benhAn, true);
    }

    private KetQuaKhamResponse toResponse(HoSoBenhAn benhAn, boolean choBacSi) {
        if (benhAn == null) {
            return null;
        }
        NgonNgu ngonNgu = NgonNgu.hienTai();
        Icd10 icd = benhAn.getIcdChinh();
        return new KetQuaKhamResponse(benhAn.getLyDoKham(), sinhHieu(benhAn), benhAn.getKhamLamSang(),
                benhAn.getChanDoan(),
                icd == null ? null : new KetQuaKhamResponse.IcdChinh(icd.getMa(), icd.ten(ngonNgu), icd.isManTinh()),
                benhAn.getChanDoanKemTheo(), benhAn.getGhiChu(), benhAn.getNgayTaiKhamDeXuat(),
                benhAn.getChiTietDonThuoc().stream()
                        .map(chiTiet -> {
                            Thuoc thuoc = chiTiet.getThuoc();
                            return new KetQuaKhamResponse.DonThuoc(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getHoatChat(),
                                    thuoc.getHamLuong(), thuoc.getDuongDung(), thuoc.donVi(ngonNgu),
                                    chiTiet.getLieuDung(), chiTiet.getSoLanMoiNgay(), chiTiet.getSoNgayDung(),
                                    chiTiet.getSoLuong(), chiTiet.getGhiChuSuDung());
                        })
                        .toList(),
                benhAn.getNgaySuaCuoi(),
                choBacSi ? benhAn.getPhienBan() : null,
                choBacSi ? benhAnProperties.hanSua(benhAn.getNgayTao()) : null,
                choBacSi ? null : benhAn.getMaTokenSoKham(),
                choBacSi ? null : soKhamProperties.lienKet(benhAn.getMaTokenSoKham()));
    }

    /** @return null nếu hồ sơ chưa có chỉ số nào */
    private static KetQuaKhamResponse.SinhHieu sinhHieu(HoSoBenhAn benhAn) {
        boolean trong = Stream.of(benhAn.getMach(), benhAn.getNhietDo(), benhAn.getHuyetApTamThu(),
                benhAn.getHuyetApTamTruong(), benhAn.getNhipTho(), benhAn.getCanNang(), benhAn.getChieuCao())
                .allMatch(chiSo -> chiSo == null);
        return trong ? null
                : new KetQuaKhamResponse.SinhHieu(benhAn.getMach(), benhAn.getNhietDo(), benhAn.getHuyetApTamThu(),
                        benhAn.getHuyetApTamTruong(), benhAn.getNhipTho(), benhAn.getCanNang(), benhAn.getChieuCao());
    }

}
