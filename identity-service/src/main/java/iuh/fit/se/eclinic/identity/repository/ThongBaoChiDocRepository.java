package iuh.fit.se.eclinic.identity.repository;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.notification.ThongBao;

/** CHỈ ĐỌC thông báo (bảng của notification-service) để kiểm tra trước khi xoá tài khoản. */
public interface ThongBaoChiDocRepository extends Repository<ThongBao, Long> {

    boolean existsByTaiKhoanId(Long taiKhoanId);

}
