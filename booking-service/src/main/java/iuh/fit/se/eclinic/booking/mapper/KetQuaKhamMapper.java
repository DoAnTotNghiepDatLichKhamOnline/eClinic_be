package iuh.fit.se.eclinic.booking.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhamResponse;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

/**
 * Chuyển hồ sơ bệnh án sang kết quả khám. Gọi trong transaction: đơn thuốc và thuốc là quan hệ lazy (nơi gọi nên tải
 * sẵn bằng HoSoBenhAnChiDocRepository#timTheoLichHenKemDonThuoc).
 */
@Component
public class KetQuaKhamMapper {

    /** @return null nếu lượt khám chưa có hồ sơ bệnh án */
    public KetQuaKhamResponse toResponse(HoSoBenhAn benhAn) {
        if (benhAn == null) {
            return null;
        }
        return new KetQuaKhamResponse(benhAn.getChanDoan(), benhAn.getGhiChu(), benhAn.getNgayTaiKhamDeXuat(),
                benhAn.getChiTietDonThuoc().stream()
                        .map(chiTiet -> new KetQuaKhamResponse.DonThuoc(chiTiet.getThuoc().getTenThuoc(),
                                chiTiet.getThuoc().getDonVi(), chiTiet.getLieuDung(), chiTiet.getSoLanMoiNgay(),
                                chiTiet.getSoNgayDung(), chiTiet.getGhiChuSuDung()))
                        .toList());
    }

}
