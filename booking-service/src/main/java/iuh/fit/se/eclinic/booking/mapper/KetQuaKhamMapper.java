package iuh.fit.se.eclinic.booking.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhamResponse;
import iuh.fit.se.eclinic.common.config.BenhAnProperties;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.RequiredArgsConstructor;

/**
 * Chuyển hồ sơ bệnh án sang kết quả khám. Gọi trong transaction: đơn thuốc và thuốc là quan hệ lazy (nơi gọi nên tải
 * sẵn bằng HoSoBenhAnChiDocRepository#timTheoLichHenKemDonThuoc).
 */
@Component
@RequiredArgsConstructor
public class KetQuaKhamMapper {

    private final BenhAnProperties benhAnProperties;

    /** Cho bệnh nhân: chỉ biết hồ sơ đã sửa lúc nào, không có phiên bản / hạn sửa. @return null nếu chưa có hồ sơ bệnh án */
    public KetQuaKhamResponse toResponse(HoSoBenhAn benhAn) {
        return toResponse(benhAn, false);
    }

    /** Cho bác sĩ (hồ sơ khám): kèm phiên bản và hạn sửa. @return null nếu chưa có hồ sơ bệnh án */
    public KetQuaKhamResponse toResponseChoBacSi(HoSoBenhAn benhAn) {
        return toResponse(benhAn, true);
    }

    private KetQuaKhamResponse toResponse(HoSoBenhAn benhAn, boolean choBacSi) {
        if (benhAn == null) {
            return null;
        }
        return new KetQuaKhamResponse(benhAn.getChanDoan(), benhAn.getGhiChu(), benhAn.getNgayTaiKhamDeXuat(),
                benhAn.getChiTietDonThuoc().stream()
                        .map(chiTiet -> {
                            Thuoc thuoc = chiTiet.getThuoc();
                            return new KetQuaKhamResponse.DonThuoc(thuoc.getId(), thuoc.getTenThuoc(), thuoc.getHoatChat(),
                                    thuoc.getHamLuong(), thuoc.getDuongDung(), thuoc.donVi(NgonNgu.hienTai()),
                                    chiTiet.getLieuDung(), chiTiet.getSoLanMoiNgay(), chiTiet.getSoNgayDung(),
                                    chiTiet.getGhiChuSuDung());
                        })
                        .toList(),
                benhAn.getNgaySuaCuoi(),
                choBacSi ? benhAn.getPhienBan() : null,
                choBacSi ? benhAnProperties.hanSua(benhAn.getNgayTao()) : null);
    }

}
