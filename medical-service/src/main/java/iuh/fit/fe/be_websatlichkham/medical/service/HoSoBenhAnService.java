package iuh.fit.fe.be_websatlichkham.medical.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.common.entity.medical.HoSoBenhAn;

public interface HoSoBenhAnService {

    HoSoBenhAn getById(Long id);

    Optional<HoSoBenhAn> findByLichHen(Long lichHenId);

    List<HoSoBenhAn> findByHoSoBenhNhan(Long hoSoBenhNhanId);

}
