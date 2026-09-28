package iuh.fit.se.eclinic.booking.service;

import java.util.List;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

public interface LichHenService {

    LichHen layTheoId(Long id);

    List<LichHen> timTheoHoSoBenhNhan(Long hoSoBenhNhanId);

    List<LichHen> timTheoBacSiVaTrangThai(Long bacSiId, TrangThaiLichHen trangThai);

}
