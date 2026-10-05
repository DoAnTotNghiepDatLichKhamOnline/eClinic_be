package iuh.fit.se.eclinic.booking.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository.SoLuotCuaCa;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import lombok.RequiredArgsConstructor;

/**
 * Chuyển ca làm việc sang dòng lịch làm việc của bác sĩ / quản trị viên. Gọi trong transaction, ca phải lấy sẵn
 * bác sĩ + tài khoản + chuyên khoa và phòng khám.
 */
@Component
@RequiredArgsConstructor
public class LichLamViecMapper {

    private final CaKhamMapper caKhamMapper;

    /** @param soLuot null khi ca không còn lượt khám nào (mọi lượt đã huỷ): các số đếm bằng 0 */
    public CaLamViecResponse toResponse(LichLamViec ca, SoLuotCuaCa soLuot) {
        return new CaLamViecResponse(ca.getId(), ca.getNgayLamViec(), ca.getGioBatDau(), ca.getGioKetThuc(),
                ca.getSoLuotToiDaMoiGio(), ca.getThoiLuongLuotPhut(), ca.getTrangThai(),
                caKhamMapper.toBacSiTomTat(ca.getBacSi()), ca.getBacSi().getChuyenKhoa().getTenChuyenKhoa(),
                caKhamMapper.toPhongKhamTomTat(ca.getPhongKham()),
                soLuot == null ? 0 : soLuot.getTongSoLuot(),
                soLuot == null ? 0 : soLuot.getSoLuotDaDat(),
                soLuot == null ? 0 : soLuot.getSoLuotConTrong());
    }

}
