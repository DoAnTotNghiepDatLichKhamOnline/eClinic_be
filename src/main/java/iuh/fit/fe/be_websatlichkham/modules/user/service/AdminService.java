package iuh.fit.fe.be_websatlichkham.modules.user.service;

import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.modules.user.entity.Admin;

public interface AdminService {

    Admin getById(Long id);

    Optional<Admin> findByUserId(Long userId);

}
