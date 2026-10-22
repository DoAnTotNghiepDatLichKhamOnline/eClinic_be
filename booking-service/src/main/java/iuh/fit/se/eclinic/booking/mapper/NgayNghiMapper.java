package iuh.fit.se.eclinic.booking.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse.QuanTriThucHien;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaKhaiBaoNgayNghiResponse.CaBiHuy;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiResponse;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.NgayNghiBenhVien;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.RequiredArgsConstructor;

/** Ngày bệnh viện nghỉ và ca bị hủy khi khai báo sang DTO. Gọi trong transaction (quản trị viên, bác sĩ lấy lười). */
@Component
@RequiredArgsConstructor
public class NgayNghiMapper {

    private final CaKhamMapper caKhamMapper;

    /** @param coId false khi xem trước: id sẽ mất khi rollback */
    public NgayNghiQuanTriResponse toQuanTri(NgayNghiBenhVien ngayNghi, boolean coId) {
        QuanTriVien quanTri = ngayNghi.getQuanTriTao();
        return new NgayNghiQuanTriResponse(coId ? ngayNghi.getId() : null, ngayNghi.getNgay(), ngayNghi.getLyDo(),
                ngayNghi.getLyDoEn(), new QuanTriThucHien(quanTri.getId(), quanTri.getTaiKhoan().getHoTen()),
                ngayNghi.getNgayTao());
    }

    /** Lý do theo ngôn ngữ của request. */
    public NgayNghiResponse toCongKhai(NgayNghiBenhVien ngayNghi) {
        return new NgayNghiResponse(ngayNghi.getNgay(), ngayNghi.lyDo(NgonNgu.hienTai()));
    }

    public CaBiHuy toCaBiHuy(LichLamViec ca, int soLichHenCanDoi) {
        return new CaBiHuy(ca.getId(), ca.getNgayLamViec(), ca.getGioBatDau(), ca.getGioKetThuc(),
                caKhamMapper.toBacSiTomTat(ca.getBacSi()), caKhamMapper.toPhongKhamTomTat(ca.getPhongKham()),
                soLichHenCanDoi);
    }

}
