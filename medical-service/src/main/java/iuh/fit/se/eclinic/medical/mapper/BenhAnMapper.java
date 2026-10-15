package iuh.fit.se.eclinic.medical.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.config.BenhAnProperties;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.Icd10;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.medical.dto.response.BenhAnResponse;
import iuh.fit.se.eclinic.medical.dto.response.Icd10Response;
import iuh.fit.se.eclinic.medical.dto.response.ThuocGoiYResponse;
import lombok.RequiredArgsConstructor;

/**
 * Chuyển hồ sơ bệnh án sang DTO. Gọi trong transaction: đơn thuốc, thuốc và mã ICD-10 là quan hệ lazy.
 */
@Component
@RequiredArgsConstructor
public class BenhAnMapper {

    private final BenhAnProperties benhAnProperties;

    public BenhAnResponse toResponse(HoSoBenhAn benhAn, LichHen lichHen) {
        return new BenhAnResponse(benhAn.getId(), lichHen.getId(), lichHen.getMaTraCuu(), lichHen.getTrangThai(),
                benhAn.getLyDoKham(), BenhAnResponse.SinhHieu.tu(benhAn), benhAn.getKhamLamSang(),
                benhAn.getChanDoan(), benhAn.getIcdChinh() == null ? null : toIcd10(benhAn.getIcdChinh()),
                benhAn.getChanDoanKemTheo(), benhAn.getGhiChu(), benhAn.getNgayTaiKhamDeXuat(),
                benhAn.getChiTietDonThuoc().stream()
                        .map(chiTiet -> {
                            Thuoc thuoc = chiTiet.getThuoc();
                            return new BenhAnResponse.DonThuoc(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getHoatChat(),
                                    thuoc.getHamLuong(), thuoc.getDuongDung(), thuoc.donVi(NgonNgu.hienTai()),
                                    chiTiet.getLieuDung(), chiTiet.getSoLanMoiNgay(), chiTiet.getSoNgayDung(),
                                    chiTiet.getSoLuong(), chiTiet.getGhiChuSuDung());
                        })
                        .toList(),
                benhAn.getNgayTao(), benhAn.getPhienBan(), benhAn.getNgaySuaCuoi(),
                benhAnProperties.hanSua(benhAn.getNgayTao()));
    }

    public ThuocGoiYResponse toGoiY(Thuoc thuoc) {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        return new ThuocGoiYResponse(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getHoatChat(), thuoc.getHamLuong(),
                thuoc.dangBaoChe(ngonNgu), thuoc.getDuongDung(), thuoc.donVi(ngonNgu));
    }

    public Icd10Response toIcd10(Icd10 icd10) {
        return new Icd10Response(icd10.getMa(), icd10.ten(NgonNgu.hienTai()), icd10.isManTinh());
    }

}
