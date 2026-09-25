package iuh.fit.fe.be_websatlichkham.identity.service;

import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.common.entity.identity.TaiKhoan;

public interface TaiKhoanService {

    TaiKhoan getById(Long id);

    Optional<TaiKhoan> findByEmail(String email);

    boolean existsByEmail(String email);

}
