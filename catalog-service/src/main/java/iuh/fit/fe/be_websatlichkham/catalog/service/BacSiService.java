package iuh.fit.fe.be_websatlichkham.catalog.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;

public interface BacSiService {

    BacSi getById(Long id);

    Optional<BacSi> findByTaiKhoanId(Long taiKhoanId);

    List<BacSi> findByChuyenKhoa(Long chuyenKhoaId);

}
