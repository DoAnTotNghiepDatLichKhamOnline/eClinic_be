package iuh.fit.fe.be_websatlichkham.modules.user.service;

import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.modules.user.entity.User;

public interface UserService {

    User getById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
