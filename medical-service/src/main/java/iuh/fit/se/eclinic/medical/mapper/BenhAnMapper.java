package iuh.fit.se.eclinic.medical.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.medical.dto.response.BenhAnResponse;
import iuh.fit.se.eclinic.medical.dto.response.ThuocGoiYResponse;

/**
 * Chuyển hồ sơ bệnh án sang DTO. Gọi trong transaction: đơn thuốc và thuốc là quan hệ lazy.
 */
@Component
public class BenhAnMapper {

    public BenhAnResponse toResponse(HoSoBenhAn benhAn, LichHen lichHen) {
        return new BenhAnResponse(benhAn.getId(), lichHen.getId(), lichHen.getMaTraCuu(), lichHen.getTrangThai(),
                benhAn.getChanDoan(), benhAn.getGhiChu(), benhAn.getNgayTaiKhamDeXuat(),
                benhAn.getChiTietDonThuoc().stream()
                        .map(chiTiet -> new BenhAnResponse.DonThuoc(chiTiet.getThuoc().getTenThuoc(),
                                chiTiet.getThuoc().getDonVi(), chiTiet.getLieuDung(), chiTiet.getSoLanMoiNgay(),
                                chiTiet.getSoNgayDung(), chiTiet.getGhiChuSuDung()))
                        .toList(),
                benhAn.getNgayTao());
    }

    public ThuocGoiYResponse toGoiY(Thuoc thuoc) {
        return new ThuocGoiYResponse(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getDonVi(), thuoc.isDaXacMinh());
    }

}
