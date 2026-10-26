package iuh.fit.se.eclinic.identity.service;

import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;

public interface QuanTriVienService {

    QuanTriVien layTheoId(Long id);

    Optional<QuanTriVien> timTheoTaiKhoanId(Long taiKhoanId);

}
