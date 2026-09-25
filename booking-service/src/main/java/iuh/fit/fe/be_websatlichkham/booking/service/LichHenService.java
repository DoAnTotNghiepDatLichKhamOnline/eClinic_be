package iuh.fit.fe.be_websatlichkham.booking.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.booking.LichHen;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLichHen;

public interface LichHenService {

    LichHen getById(Long id);

    List<LichHen> findByHoSoBenhNhan(Long hoSoBenhNhanId);

    List<LichHen> findByBacSiAndTrangThai(Long bacSiId, TrangThaiLichHen trangThai);

}
