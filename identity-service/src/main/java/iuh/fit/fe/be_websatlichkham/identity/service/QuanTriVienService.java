package iuh.fit.fe.be_websatlichkham.identity.service;

import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.common.entity.identity.QuanTriVien;

public interface QuanTriVienService {

    QuanTriVien getById(Long id);

    Optional<QuanTriVien> findByTaiKhoanId(Long taiKhoanId);

}
