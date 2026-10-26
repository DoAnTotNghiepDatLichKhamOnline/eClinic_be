package iuh.fit.se.eclinic.medical.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

public interface HoSoBenhAnService {

    HoSoBenhAn layTheoId(Long id);

    Optional<HoSoBenhAn> timTheoLichHen(Long lichHenId);

    List<HoSoBenhAn> timTheoHoSoBenhNhan(Long hoSoBenhNhanId);

}
