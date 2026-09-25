package iuh.fit.fe.be_websatlichkham.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.notification.ThongBao;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {

    List<ThongBao> findByTaiKhoanIdOrderByNgayTaoDesc(Long taiKhoanId);

    long countByTaiKhoanIdAndDaDocFalse(Long taiKhoanId);

}
