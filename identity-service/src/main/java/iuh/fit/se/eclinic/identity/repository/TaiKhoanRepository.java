package iuh.fit.se.eclinic.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {

    Optional<TaiKhoan> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByVaiTro(VaiTro vaiTro);

}
