package iuh.fit.se.eclinic.booking.service;

import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;

public interface HoSoBenhNhanService {

    HoSoBenhNhan layTheoId(Long id);

    Optional<HoSoBenhNhan> timTheoTaiKhoanId(Long taiKhoanId);

    Optional<HoSoBenhNhan> timTheoCccd(String cccd);

}
