package iuh.fit.se.eclinic.identity.service;

import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

public interface TaiKhoanService {

    TaiKhoan layTheoId(Long id);

    Optional<TaiKhoan> timTheoEmail(String email);

    boolean tonTaiEmail(String email);

}
