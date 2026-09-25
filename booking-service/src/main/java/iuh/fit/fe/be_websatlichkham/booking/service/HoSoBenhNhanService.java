package iuh.fit.fe.be_websatlichkham.booking.service;

import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.common.entity.booking.HoSoBenhNhan;

public interface HoSoBenhNhanService {

    HoSoBenhNhan getById(Long id);

    Optional<HoSoBenhNhan> findByTaiKhoanId(Long taiKhoanId);

    Optional<HoSoBenhNhan> findByCccd(String cccd);

}
