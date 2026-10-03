package iuh.fit.se.eclinic.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.notification.ThongBao;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {

    List<ThongBao> findByTaiKhoanIdOrderByNgayTaoDesc(Long taiKhoanId);

    long countByTaiKhoanIdAndDaDocFalse(Long taiKhoanId);

}
